package com.example.milvus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 配置属性类
 * 用于读取 application.yml 中的 Milvus 相关配置
 *
 * @author example
 */
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    private String host = "localhost";
    private int port = 19530;
    private String collectionName = "demo_collection";
    private int dimension = 128;
    private String indexType = "IVF_FLAT";
    private String metricType = "L2";
    private int nlist = 1024;
    private int nprobe = 10;

    // Getters and Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public int getNlist() {
        return nlist;
    }

    public void setNlist(int nlist) {
        this.nlist = nlist;
    }

    public int getNprobe() {
        return nprobe;
    }

    public void setNprobe(int nprobe) {
        this.nprobe = nprobe;
    }
}
