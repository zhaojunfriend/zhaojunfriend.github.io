package com.example.milvus.model;

import java.util.List;

/**
 * 向量搜索请求模型
 *
 * @author example
 */
public class SearchRequest {

    /**
     * 查询向量
     */
    private List<Float> queryVector;

    /**
     * 返回结果数量
     */
    private int topK = 10;

    public SearchRequest() {
    }

    public SearchRequest(List<Float> queryVector, int topK) {
        this.queryVector = queryVector;
        this.topK = topK;
    }

    public List<Float> getQueryVector() {
        return queryVector;
    }

    public void setQueryVector(List<Float> queryVector) {
        this.queryVector = queryVector;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
