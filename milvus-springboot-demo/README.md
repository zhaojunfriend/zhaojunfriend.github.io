# Milvus Spring Boot 集成示例

本项目演示了如何将 Milvus 向量数据库与 Spring Boot 进行集成，提供完整的向量存储和相似性搜索功能。

## 项目结构

```
milvus-springboot-demo/
├── pom.xml                          # Maven 配置文件
└── src/main/
    ├── java/com/example/milvus/
    │   ├── MilvusSpringBootApplication.java  # 应用入口
    │   ├── config/
    │   │   ├── MilvusConfig.java            # Milvus 客户端配置
    │   │   └── MilvusProperties.java        # 配置属性类
    │   ├── controller/
    │   │   └── MilvusController.java        # REST API 控制器
    │   ├── model/
    │   │   ├── VectorData.java              # 向量数据模型
    │   │   └── SearchRequest.java           # 搜索请求模型
    │   └── service/
    │       └── MilvusService.java           # Milvus 操作服务
    └── resources/
        └── application.yml                   # 应用配置文件
```

## 环境要求

- JDK 17+
- Maven 3.6+
- Milvus 2.x (本地或远程服务器)
- Docker (可选，用于运行 Milvus)

## 快速开始

### 1. 启动 Milvus 服务

使用 Docker Compose 快速启动 Milvus：

```bash
# 下载 docker-compose 文件
wget https://github.com/milvus-io/milvus/releases/download/v2.3.4/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动服务
docker-compose up -d
```

或者使用 Docker 直接运行：

```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.3.4 milvus run standalone
```

### 2. 配置项目

编辑 `src/main/resources/application.yml`：

```yaml
milvus:
  host: localhost        # Milvus 服务器地址
  port: 19530           # Milvus 端口
  collection-name: demo_collection  # 默认集合名称
  dimension: 128        # 向量维度
  index-type: IVF_FLAT  # 索引类型
  metric-type: L2       # 度量类型
  nlist: 1024          # 索引参数
```

### 3. 构建并运行

```bash
cd milvus-springboot-demo

# 构建项目
mvn clean package

# 运行应用
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## API 接口

### 集合管理

#### 创建集合
```bash
POST /api/milvus/collection/create?collectionName=my_collection&dimension=128
```

#### 检查集合是否存在
```bash
GET /api/milvus/collection/{collectionName}/exists
```

#### 创建索引
```bash
POST /api/milvus/collection/{collectionName}/index
```

#### 加载集合到内存
```bash
POST /api/milvus/collection/{collectionName}/load
```

#### 释放集合
```bash
POST /api/milvus/collection/{collectionName}/release
```

#### 删除集合
```bash
DELETE /api/milvus/collection/{collectionName}
```

### 数据操作

#### 插入向量数据
```bash
POST /api/milvus/collection/{collectionName}/insert
Content-Type: application/json

[
  {
    "vector": [0.1, 0.2, 0.3, ...],
    "content": "这是一段文本内容"
  }
]
```

#### 向量相似性搜索
```bash
POST /api/milvus/collection/{collectionName}/search
Content-Type: application/json

{
  "queryVector": [0.1, 0.2, 0.3, ...],
  "topK": 10
}
```

### 辅助接口

#### 初始化默认集合
```bash
POST /api/milvus/init
```

#### 插入测试数据
```bash
POST /api/milvus/collection/{collectionName}/test-data?count=100
```

#### 健康检查
```bash
GET /api/milvus/health
```

## 使用示例

### 完整流程示例

```bash
# 1. 初始化默认集合
curl -X POST http://localhost:8080/api/milvus/init

# 2. 插入测试数据
curl -X POST "http://localhost:8080/api/milvus/collection/demo_collection/test-data?count=100"

# 3. 搜索相似向量
curl -X POST http://localhost:8080/api/milvus/collection/demo_collection/search \
  -H "Content-Type: application/json" \
  -d '{
    "queryVector": [0.1, 0.2, 0.3, 0.4, 0.5, ...],
    "topK": 5
  }'
```

### Java 代码示例

```java
@Autowired
private MilvusService milvusService;

// 创建集合
milvusService.createCollection("my_collection", 128);
milvusService.createIndex("my_collection");
milvusService.loadCollection("my_collection");

// 插入数据
List<VectorData> dataList = new ArrayList<>();
dataList.add(new VectorData(null, Arrays.asList(0.1f, 0.2f, ...), "内容1"));
dataList.add(new VectorData(null, Arrays.asList(0.3f, 0.4f, ...), "内容2"));
List<Long> ids = milvusService.insert("my_collection", dataList);

// 搜索
List<Float> queryVector = Arrays.asList(0.1f, 0.2f, ...);
List<VectorData> results = milvusService.search("my_collection", queryVector, 10);
```

## 配置说明

### 索引类型 (index-type)

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| FLAT | 暴力搜索 | 小数据量，100% 召回率 |
| IVF_FLAT | 倒排索引 | 中等数据量 |
| IVF_SQ8 | 量化压缩 | 大数据量，节省内存 |
| IVF_PQ | 乘积量化 | 超大数据量 |
| HNSW | 图索引 | 高性能搜索 |

### 度量类型 (metric-type)

| 类型 | 说明 |
|------|------|
| L2 | 欧氏距离 |
| IP | 内积 |
| COSINE | 余弦相似度 |

## 注意事项

1. **Milvus 连接**: 确保 Milvus 服务已启动并可访问
2. **向量维度**: 所有向量必须与创建集合时指定的维度一致
3. **内存管理**: 搜索前需要将集合加载到内存，不用时可以释放
4. **索引**: 创建索引可以显著提升搜索性能

## 常见问题

**Q: 连接 Milvus 失败？**
A: 检查 Milvus 服务是否启动，以及配置的 host 和 port 是否正确。

**Q: 搜索结果为空？**
A: 确保集合已加载到内存 (`loadCollection`)，且集合中有数据。

**Q: 向量维度不匹配？**
A: 插入和搜索的向量维度必须与创建集合时指定的维度一致。

## 参考资料

- [Milvus 官方文档](https://milvus.io/docs)
- [Milvus Java SDK](https://github.com/milvus-io/milvus-sdk-java)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
