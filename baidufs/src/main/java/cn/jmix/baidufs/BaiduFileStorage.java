package cn.jmix.baidufs;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.*;
import io.jmix.core.*;
import io.jmix.core.annotation.Internal;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Internal
@Component("baidufs_FileStorage")
public class BaiduFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(BaiduFileStorage.class);
    public static final String DEFAULT_STORAGE_NAME = "bos";

    protected String storageName;

    @Autowired
    protected BaiduFileStorageProperties properties;

    boolean useConfigurationProperties = true;

    protected String accessKey;
    protected String secretAccessKey;
    protected String region;
    protected String bucket;
    protected int chunkSize;
    protected String endpointUrl;

    @Autowired
    protected TimeSource timeSource;

    protected AtomicReference<BosClient> clientReference = new AtomicReference<>();

    public BaiduFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public BaiduFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows you to override {@link BaiduFileStorageProperties}.
     */
    public BaiduFileStorage(
            String storageName,
            String accessKey,
            String secretAccessKey,
            String bucket,
            int chunkSize,
            @Nullable String endpointUrl) {
        this.useConfigurationProperties = false;
        this.storageName = storageName;
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
    }

    @EventListener
    public void initOssClient(ApplicationStartedEvent event) {
        refreshOssClient();
    }

    protected void refreshProperties() {
        if (useConfigurationProperties) {
            this.accessKey = properties.getAccessKey();
            this.secretAccessKey = properties.getSecretAccessKey();
            this.bucket = properties.getBucket();
            this.chunkSize = properties.getChunkSize();
            this.endpointUrl = properties.getEndpointUrl();
        }
    }


    public void refreshOssClient() {
        refreshProperties();
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(accessKey,secretAccessKey));
        config.setEndpoint(endpointUrl);
        BosClient  ossClient = new BosClient (config);
        clientReference.set(ossClient);
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    protected String createFileKey(String fileName) {
        return createDateDir() + "/" + createUuidFilename(fileName);
    }

    protected String createDateDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSource.currentTimestamp());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid().toString() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }

    private String claimUploadId(String objectName) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, objectName);
        InitiateMultipartUploadResponse result = clientReference.get().initiateMultipartUpload(request);
        return result.getUploadId();
    }

    private void completeMultipartUpload(List<PartETag> partETags, String objectName, String uploadId) {
        // Make part numbers in ascending order
        Collections.sort(partETags, new Comparator<PartETag>() {
            @Override
            public int compare(PartETag p1, PartETag p2) {
                return p1.getPartNumber() - p2.getPartNumber();
            }
        });
        log.info("Completing to upload multiparts\n");
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, objectName, uploadId, partETags);
        clientReference.get().completeMultipartUpload(completeMultipartUploadRequest);
    }

    private void listAllParts(String objectName, String uploadId) {
        log.debug("Listing all parts......");
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucket, objectName, uploadId);
        ListPartsResponse partListing = clientReference.get().listParts(listPartsRequest);
        int partCount = partListing.getParts().size();
        for (int i = 0; i < partCount; i++) {
            PartSummary partSummary = partListing.getParts().get(i);
            log.debug("\tPart#" + partSummary.getPartNumber() + ", ETag=" + partSummary.getETag());
        }
        log.debug("\n");
    }

    @Override
    public FileRef saveStream(String fileName, InputStream inputStream) {
        String fileKey = createFileKey(fileName);
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            BosClient client = clientReference.get();
            int chunkSizeBytes = this.chunkSize * 1024;
            List<PartETag> partETags = new ArrayList<>();

            String uploadId = claimUploadId(fileKey);
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            int partCount = 0;
            for (int i = 0; i * chunkSizeBytes < data.length; i++) {
                partCount++;
                int partNumber = i + 1;
                int endChunkPosition = Math.min(partNumber * chunkSizeBytes, data.length);
                byte[] chunkBytes = getChunkBytes(data, i * chunkSizeBytes, endChunkPosition);

                PartUploader partUploader =
                        new PartUploader(client, partETags, chunkBytes, fileKey, bucket, chunkBytes.length, partNumber, uploadId);
                executorService.execute(partUploader);
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, "Uploading file to bos failed", e);
                } finally {
                    executorService.shutdownNow();
                }
            }
            if (partETags.size() != partCount) {
                throw new IllegalStateException("Upload multiparts fail due to some parts are not finished yet");
            } else {
                log.info("Succeed to complete multiparts into an object named " + fileKey + "\n");
            }

            listAllParts(fileKey, uploadId);
            completeMultipartUpload(partETags, fileKey, uploadId);
            return new FileRef(getStorageName(), fileKey, fileName);
        } catch (IOException e) {
            String message = String.format("Could not save file %s.", fileName);
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    protected byte[] getChunkBytes(byte[] data, int start, int end) {
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(data, start, chunkBytes, 0, end - start);
        return chunkBytes;
    }

    @Override
    public InputStream openStream(FileRef reference) {
        try {
            BosClient client = clientReference.get();
            BosObject object = client.getObject(new GetObjectRequest(bucket, reference.getPath()));
            return object.getObjectContent();
        } catch (Exception e) {
            String message = String.format("Could not load file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public void removeFile(FileRef reference) {
        try {
            BosClient client = clientReference.get();
            DeleteObjectRequest request = new DeleteObjectRequest(bucket,reference.getPath());
            client.deleteObject(request);
        } catch (Exception e) {
            String message = String.format("Could not delete file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public boolean fileExists(FileRef reference) {
        BosClient client = clientReference.get();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucket);
        listObjectsRequest.setPrefix(reference.getPath());
        listObjectsRequest.setMaxKeys(1);
        ListObjectsResponse objectListing = client.listObjects(listObjectsRequest);
        List<BosObjectSummary> summaries = objectListing.getContents();
        return summaries.size() > 0;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEndpointUrl(@Nullable String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
