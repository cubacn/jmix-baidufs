package test_support;


import cn.jmix.baidufs.BaiduFileStorageConfiguration;
import io.jmix.core.annotation.JmixModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:/test_support/test-app.properties")
@JmixModule(dependsOn = BaiduFileStorageConfiguration.class)
public class BaiduFileStorageTestConfiguration {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
