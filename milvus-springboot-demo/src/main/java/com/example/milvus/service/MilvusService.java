package com.example.milvus.service;

import com.example.milvus.config.MilvusProperties;
import com.example.milvus.model.VectorData;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 服务类
 * 提供向量数据库的 CRUD 操作
 *
 * @author example
 */
@Service
public class MilvusService {

    private static final Logger log = LoggerFactory.getLogger(MilvusService.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_CONTENT = "content";

    private final MilvusServiceClient milvusClient;
    private final MilvusProperties milvusProperties;

    public MilvusService(MilvusServiceClient milvusClient, MilvusProperties milvusProperties) {
        this.milvusClient = milvusClient;
        this.milvusProperties = milvusProperties;
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return 是否存在
     */
    public boolean hasCollection(String collectionName) {
        R<Boolean> response = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("检查集合失败: {}", response.getMessage());
            return false;
        }
        return response.getData();
    }

    /**
     * 创建集合
     *
     * @param collectionName 集合名称
     * @param dimension      向量维度
     * @return 是否创建成功
     */
    public boolean createCollection(String collectionName, int dimension) {
        if (hasCollection(collectionName)) {
            log.info("集合已存在: {}", collectionName);
            return true;
        }

        // 定义字段
        FieldType idField = FieldType.newBuilder()
                .withName(FIELD_ID)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName(FIELD_VECTOR)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName(FIELD_CONTENT)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        // 创建集合
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Demo collection for vector search")
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(contentField)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(createParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("创建集合失败: {}", response.getMessage());
            return false;
        }

        log.info("集合创建成功: {}", collectionName);
        return true;
    }

    /**
     * 创建索引
     *
     * @param collectionName 集合名称
     * @return 是否创建成功
     */
    public boolean createIndex(String collectionName) {
        IndexType indexType = IndexType.valueOf(milvusProperties.getIndexType());
        MetricType metricType = MetricType.valueOf(milvusProperties.getMetricType());

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(FIELD_VECTOR)
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam("{\"nlist\":" + milvusProperties.getNlist() + "}")
                .withSyncMode(Boolean.TRUE)
                .build();

        R<RpcStatus> response = milvusClient.createIndex(indexParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("创建索引失败: {}", response.getMessage());
            return false;
        }

        log.info("索引创建成功: {}", collectionName);
        return true;
    }

    /**
     * 加载集合到内存
     *
     * @param collectionName 集合名称
     * @return 是否加载成功
     */
    public boolean loadCollection(String collectionName) {
        R<RpcStatus> response = milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withSyncLoad(true)
                        .build()
        );

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("加载集合失败: {}", response.getMessage());
            return false;
        }

        log.info("集合加载成功: {}", collectionName);
        return true;
    }

    /**
     * 释放集合
     *
     * @param collectionName 集合名称
     * @return 是否释放成功
     */
    public boolean releaseCollection(String collectionName) {
        R<RpcStatus> response = milvusClient.releaseCollection(
                ReleaseCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("释放集合失败: {}", response.getMessage());
            return false;
        }

        log.info("集合释放成功: {}", collectionName);
        return true;
    }

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    public boolean dropCollection(String collectionName) {
        R<RpcStatus> response = milvusClient.dropCollection(
                DropCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("删除集合失败: {}", response.getMessage());
            return false;
        }

        log.info("集合删除成功: {}", collectionName);
        return true;
    }

    /**
     * 插入向量数据
     *
     * @param collectionName 集合名称
     * @param vectorDataList 向量数据列表
     * @return 插入的 ID 列表
     */
    public List<Long> insert(String collectionName, List<VectorData> vectorDataList) {
        if (vectorDataList == null || vectorDataList.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Float>> vectors = new ArrayList<>();
        List<String> contents = new ArrayList<>();

        for (VectorData data : vectorDataList) {
            vectors.add(data.getVector());
            contents.add(data.getContent() != null ? data.getContent() : "");
        }

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field(FIELD_VECTOR, vectors),
                new InsertParam.Field(FIELD_CONTENT, contents)
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> response = milvusClient.insert(insertParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("插入数据失败: {}", response.getMessage());
            return Collections.emptyList();
        }

        List<Long> ids = response.getData().getIDs().getIntId().getDataList();
        log.info("成功插入 {} 条数据", ids.size());
        return ids;
    }

    /**
     * 向量相似性搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @return 搜索结果列表
     */
    public List<VectorData> search(String collectionName, List<Float> queryVector, int topK) {
        MetricType metricType = MetricType.valueOf(milvusProperties.getMetricType());

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(metricType)
                .withOutFields(Arrays.asList(FIELD_ID, FIELD_CONTENT))
                .withTopK(topK)
                .withVectors(Collections.singletonList(queryVector))
                .withVectorFieldName(FIELD_VECTOR)
                .withParams("{\"nprobe\":" + milvusProperties.getNprobe() + "}")
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("搜索失败: {}", response.getMessage());
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<VectorData> results = new ArrayList<>();

        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
            VectorData vectorData = new VectorData();
            vectorData.setId(idScore.getLongID());
            vectorData.setScore(idScore.getScore());

            // 获取 content 字段
            Object content = wrapper.getRowRecords(0).get(i).get(FIELD_CONTENT);
            if (content != null) {
                vectorData.setContent(content.toString());
            }

            results.add(vectorData);
        }

        log.info("搜索返回 {} 条结果", results.size());
        return results;
    }

    /**
     * 初始化默认集合
     * 创建集合、索引并加载到内存
     */
    public void initDefaultCollection() {
        String collectionName = milvusProperties.getCollectionName();
        int dimension = milvusProperties.getDimension();

        if (createCollection(collectionName, dimension)) {
            createIndex(collectionName);
            loadCollection(collectionName);
        }
    }
}
