package cn.jmix.baidufs;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@ManagedResource(description = "Manages BOS file storage client", objectName = "jmix.baidufs:type=BaiduFileStorage")
@Component("baidufs_BaiduFileStorageManagementFacade")
public class BaiduStorageManagementFacade {
    @Autowired
    protected FileStorageLocator fileStorageLocator;

    @ManagedOperation(description = "Refresh BOS file storage client")
    public String refreshBosClient() {
        FileStorage fileStorage = fileStorageLocator.getDefault();
        if (fileStorage instanceof BaiduFileStorage) {
            ((BaiduFileStorage) fileStorage).refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an BOS file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh BOS client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "BOS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "BOS secret access key")})
    public String refreshBosClient(String storageName, String accessKey, String secretAccessKey) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof BaiduFileStorage) {
            BaiduFileStorage baiduFileStorage = (BaiduFileStorage) fileStorage;
            baiduFileStorage.setAccessKey(accessKey);
            baiduFileStorage.setSecretAccessKey(secretAccessKey);
            baiduFileStorage.refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an BOS file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh BOS file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "BOS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "BOS secret access key"),
            @ManagedOperationParameter(name = "bucket", description = "BOS bucket name"),
            @ManagedOperationParameter(name = "chunkSize", description = "BOS chunk size (kB)"),
            @ManagedOperationParameter(name = "endpointUrl", description = "Optional custom BOS storage endpoint URL")})
    public String refreshBosClient(String storageName, String accessKey, String secretAccessKey,
                                  String region, String bucket, int chunkSize, @Nullable String endpointUrl) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof BaiduFileStorage) {
            BaiduFileStorage baiduFileStorage = (BaiduFileStorage) fileStorage;
            baiduFileStorage.setAccessKey(accessKey);
            baiduFileStorage.setSecretAccessKey(secretAccessKey);
            baiduFileStorage.setRegion(region);
            baiduFileStorage.setBucket(bucket);
            baiduFileStorage.setChunkSize(chunkSize);
            baiduFileStorage.setEndpointUrl(endpointUrl);
            baiduFileStorage.refreshOssClient();
            return "Refreshed successfully";
        }
        return "Not an BOS file storage - refresh attempt ignored";
    }
}
