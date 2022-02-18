package cn.jmix.baidufs.autoconfigure;

import cn.jmix.baidufs.BaiduFileStorageConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({BaiduFileStorageConfiguration.class})
public class BaidufsAutoConfiguration {
}

