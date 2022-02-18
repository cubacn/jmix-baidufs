package cn.jmix.baidufs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jmix.baidufs")
@ConstructorBinding
public class BaiduFileStorageProperties {
    String accessKey;
    String secretAccessKey;
    String bucket;
    int chunkSize;
    String endpointUrl;

    public BaiduFileStorageProperties(
            String accessKey,
            String secretAccessKey,
            String bucket,
            @DefaultValue("8192") int chunkSize,
            @DefaultValue("") String endpointUrl) {
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
    }

    /**
     *  access key.
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * secret access key.
     */
    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    /**
     *  bucket name.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     *  chunk size (kB).
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Return  storage endpoint URL.
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }
}
