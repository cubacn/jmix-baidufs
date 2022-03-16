# Jmix 百度云对象存储

此扩展提供了一个 FileStorage 实现，可将基于Jmix文件引用(FileRef)数据存储到 [百度云对象存储](https://cloud.baidu.com/doc/BOS/index.html) 。

## 安装

下表展示了扩展组件的版本以及兼容的Jmix平台版本。

| Jmix 版本       | 扩展组件版本     | Implementation                             |
|---------------|----------------|--------------------------------------------|
| 1.1.\*, 1.2.* | 1.0.0          | cn.jmix:jmixcn-baidufs-starter:1.0.0                |

`build.gradle` 文件中添加依赖:

```gradle
implementation 'cn.jmix:jmixcn-baidufs-starter:1.0.0'
```

# 配置
在 `application.properties` 配置文件中添加以下属性配置:

| 属性名                         | 默认值   | 说明                                                                                                          |
|-------------------------------|---------|----------------------------------------|
| jmix.baidufs.accessKey        |         | Access Key ID用于标示用户                |              
| jmix.baidufs.secretAccessKey  |         | Secret Access Key 密钥                  |
| jmix.baidufs.bucket           |         | 存储桶                                  |
| jmix.baidufs.region           |         | 地域                                    |
| jmix.baidufs.chunkSize        |   8192  | 块大小，单位是KB                          |
| jmix.baidufs.endpointUrl      |         | 服务端点地址                              |

