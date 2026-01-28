package com.example.milvus.controller;

import com.example.milvus.config.MilvusProperties;
import com.example.milvus.model.SearchRequest;
import com.example.milvus.model.VectorData;
import com.example.milvus.service.MilvusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Milvus REST API 控制器
 * 提供向量数据库操作的 HTTP 接口
 *
 * @author example
 */
@RestController
@RequestMapping("/api/milvus")
public class MilvusController {

    private static final Logger log = LoggerFactory.getLogger(MilvusController.class);

    private final MilvusService milvusService;
    private final MilvusProperties milvusProperties;

    public MilvusController(MilvusService milvusService, MilvusProperties milvusProperties) {
        this.milvusService = milvusService;
        this.milvusProperties = milvusProperties;
    }

    /**
     * 创建集合
     *
     * @param collectionName 集合名称（可选，默认使用配置中的名称）
     * @param dimension      向量维度（可选，默认使用配置中的维度）
     * @return 创建结果
     */
    @PostMapping("/collection/create")
    public ResponseEntity<Map<String, Object>> createCollection(
            @RequestParam(required = false) String collectionName,
            @RequestParam(required = false) Integer dimension) {

        String name = collectionName != null ? collectionName : milvusProperties.getCollectionName();
        int dim = dimension != null ? dimension : milvusProperties.getDimension();

        // Validate dimension
        if (dim <= 0 || dim > 32768) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Dimension must be between 1 and 32768");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("Create collection request: name={}, dimension={}", name, dim);

        boolean success = milvusService.createCollection(name, dim);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("collectionName", name);
        result.put("dimension", dim);

        if (!success) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return 是否存在
     */
    @GetMapping("/collection/{collectionName}/exists")
    public ResponseEntity<Map<String, Object>> hasCollection(@PathVariable String collectionName) {
        boolean exists = milvusService.hasCollection(collectionName);

        Map<String, Object> result = new HashMap<>();
        result.put("collectionName", collectionName);
        result.put("exists", exists);

        return ResponseEntity.ok(result);
    }

    /**
     * 创建索引
     *
     * @param collectionName 集合名称
     * @return 创建结果
     */
    @PostMapping("/collection/{collectionName}/index")
    public ResponseEntity<Map<String, Object>> createIndex(@PathVariable String collectionName) {
        boolean success = milvusService.createIndex(collectionName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("collectionName", collectionName);
        result.put("indexType", milvusProperties.getIndexType());

        return ResponseEntity.ok(result);
    }

    /**
     * 加载集合到内存
     *
     * @param collectionName 集合名称
     * @return 加载结果
     */
    @PostMapping("/collection/{collectionName}/load")
    public ResponseEntity<Map<String, Object>> loadCollection(@PathVariable String collectionName) {
        boolean success = milvusService.loadCollection(collectionName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("collectionName", collectionName);

        return ResponseEntity.ok(result);
    }

    /**
     * 释放集合
     *
     * @param collectionName 集合名称
     * @return 释放结果
     */
    @PostMapping("/collection/{collectionName}/release")
    public ResponseEntity<Map<String, Object>> releaseCollection(@PathVariable String collectionName) {
        boolean success = milvusService.releaseCollection(collectionName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("collectionName", collectionName);

        return ResponseEntity.ok(result);
    }

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     * @return 删除结果
     */
    @DeleteMapping("/collection/{collectionName}")
    public ResponseEntity<Map<String, Object>> dropCollection(@PathVariable String collectionName) {
        boolean success = milvusService.dropCollection(collectionName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("collectionName", collectionName);

        return ResponseEntity.ok(result);
    }

    /**
     * 插入向量数据
     *
     * @param collectionName 集合名称
     * @param vectorDataList 向量数据列表
     * @return 插入结果
     */
    @PostMapping("/collection/{collectionName}/insert")
    public ResponseEntity<Map<String, Object>> insert(
            @PathVariable String collectionName,
            @RequestBody List<VectorData> vectorDataList) {

        // Validate input
        if (vectorDataList == null || vectorDataList.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Vector data list cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate vector dimensions
        int expectedDimension = milvusProperties.getDimension();
        for (VectorData data : vectorDataList) {
            if (data.getVector() == null || data.getVector().size() != expectedDimension) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "All vectors must have dimension " + expectedDimension);
                return ResponseEntity.badRequest().body(error);
            }
        }

        log.info("Insert request: collection={}, count={}", collectionName, vectorDataList.size());

        List<Long> ids = milvusService.insert(collectionName, vectorDataList);

        Map<String, Object> result = new HashMap<>();
        result.put("success", !ids.isEmpty());
        result.put("insertedIds", ids);
        result.put("count", ids.size());

        if (ids.isEmpty()) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 向量相似性搜索
     *
     * @param collectionName 集合名称
     * @param searchRequest  搜索请求
     * @return 搜索结果
     */
    @PostMapping("/collection/{collectionName}/search")
    public ResponseEntity<Map<String, Object>> search(
            @PathVariable String collectionName,
            @RequestBody SearchRequest searchRequest) {

        // Validate query vector
        int expectedDimension = milvusProperties.getDimension();
        if (searchRequest.getQueryVector() == null || 
            searchRequest.getQueryVector().size() != expectedDimension) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Query vector must have dimension " + expectedDimension);
            return ResponseEntity.badRequest().body(error);
        }

        // Validate topK
        if (searchRequest.getTopK() <= 0 || searchRequest.getTopK() > 1000) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "topK must be between 1 and 1000");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("Search request: collection={}, topK={}", collectionName, searchRequest.getTopK());

        List<VectorData> results = milvusService.search(
                collectionName,
                searchRequest.getQueryVector(),
                searchRequest.getTopK()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("results", results);
        response.put("count", results.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 初始化默认集合
     * 创建集合、索引并加载到内存
     *
     * @return 初始化结果
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initDefaultCollection() {
        milvusService.initDefaultCollection();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("collectionName", milvusProperties.getCollectionName());
        result.put("dimension", milvusProperties.getDimension());

        return ResponseEntity.ok(result);
    }

    /**
     * 插入测试数据
     * 生成随机向量用于测试
     *
     * @param collectionName 集合名称
     * @param count          数据条数
     * @return 插入结果
     */
    @PostMapping("/collection/{collectionName}/test-data")
    public ResponseEntity<Map<String, Object>> insertTestData(
            @PathVariable String collectionName,
            @RequestParam(defaultValue = "100") int count) {

        // Validate count
        if (count <= 0 || count > 10000) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Count must be between 1 and 10000");
            return ResponseEntity.badRequest().body(error);
        }

        int dimension = milvusProperties.getDimension();
        Random random = new Random();
        List<VectorData> vectorDataList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            List<Float> vector = new ArrayList<>();
            for (int j = 0; j < dimension; j++) {
                vector.add(random.nextFloat());
            }
            VectorData data = new VectorData(null, vector, "Test content " + i);
            vectorDataList.add(data);
        }

        List<Long> ids = milvusService.insert(collectionName, vectorDataList);

        Map<String, Object> result = new HashMap<>();
        result.put("success", !ids.isEmpty());
        result.put("insertedCount", ids.size());
        result.put("dimension", dimension);

        if (ids.isEmpty()) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "milvus-springboot-demo");

        return ResponseEntity.ok(result);
    }
}
