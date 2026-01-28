package com.example.milvus.model;

import java.util.List;

/**
 * 向量数据模型
 * 用于封装向量数据的请求和响应
 *
 * @author example
 */
public class VectorData {

    /**
     * 向量 ID
     */
    private Long id;

    /**
     * 向量数据
     */
    private List<Float> vector;

    /**
     * 关联的文本内容（可选）
     */
    private String content;

    /**
     * 相似度分数（搜索结果中使用）
     */
    private Float score;

    public VectorData() {
    }

    public VectorData(Long id, List<Float> vector) {
        this.id = id;
        this.vector = vector;
    }

    public VectorData(Long id, List<Float> vector, String content) {
        this.id = id;
        this.vector = vector;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "id=" + id +
                ", vector=" + (vector != null ? "[" + vector.size() + " dimensions]" : "null") +
                ", content='" + content + '\'' +
                ", score=" + score +
                '}';
    }
}
