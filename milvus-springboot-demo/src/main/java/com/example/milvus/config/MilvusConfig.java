package com.example.milvus.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端配置类
 * 用于创建和管理 Milvus 连接
 *
 * @author example
 */
@Configuration
public class MilvusConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);

    private final MilvusProperties milvusProperties;
    private MilvusServiceClient milvusClient;

    public MilvusConfig(MilvusProperties milvusProperties) {
        this.milvusProperties = milvusProperties;
    }

    /**
     * 创建 Milvus 客户端 Bean
     *
     * @return MilvusServiceClient 实例
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("Connecting to Milvus server: {}:{}", milvusProperties.getHost(), milvusProperties.getPort());

        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusProperties.getHost())
                .withPort(milvusProperties.getPort())
                .build();

        milvusClient = new MilvusServiceClient(connectParam);
        log.info("Milvus client connected successfully");

        return milvusClient;
    }

    /**
     * 应用关闭时释放 Milvus 连接
     */
    @PreDestroy
    public void closeMilvusConnection() {
        if (milvusClient != null) {
            log.info("Closing Milvus connection...");
            milvusClient.close();
            log.info("Milvus connection closed");
        }
    }
}
