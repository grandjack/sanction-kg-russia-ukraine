# Sanction-KG Risk System - 后端API接口设计文档

> **技术栈**: Java 11 + Spring Boot 2.7+ + MySQL 8.0+ + Neo4j 4.4+  
> **架构**: RESTful API  
> **文档版本**: v1.0  
> **日期**: 2026-02-06

---

## 📋 目录

1. [总体设计](#1-总体设计)
2. [API接口清单](#2-api接口清单)
3. [数据模型](#3-数据模型)
4. [接口详细设计](#4-接口详细设计)
5. [数据库设计](#5-数据库设计)
6. [技术实现建议](#6-技术实现建议)

---

## 1. 总体设计

### 1.1 架构分层

```
┌─────────────────────────────────────┐
│   Controller Layer (REST API)       │  Spring MVC
├─────────────────────────────────────┤
│   Service Layer (业务逻辑)          │  @Service
├─────────────────────────────────────┤
│   Repository Layer (数据访问)        │  Spring Data JPA + Neo4j OGM
├─────────────────────────────────────┤
│   MySQL (关系数据)  │  Neo4j (图数据)│
└─────────────────────────────────────┘
```

### 1.2 技术组件

| 组件 | 技术选型 | 用途 |
|------|---------|------|
| Web框架 | Spring Boot 2.7.x | REST API |
| ORM | Spring Data JPA | MySQL访问 |
| 图数据库 | Spring Data Neo4j | 图谱访问 |
| 缓存 | Redis (可选) | 热点数据缓存 |
| 任务调度 | Spring @Async / Quartz | 异步任务 |
| 文档 | Swagger/OpenAPI 3.0 | API文档 |
| 日志 | SLF4J + Logback | 日志管理 |

### 1.3 通用响应格式

所有API返回统一的JSON格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

错误响应：
```json
{
  "code": 400,
  "message": "参数错误: entityId不能为空",
  "data": null,
  "timestamp": "2026-02-06T14:23:45Z",
  "path": "/api/entities/detail"
}
```

### 1.4 状态码规范

| 状态码 | 含义 | 使用场景 |
|-------|------|---------|
| 200 | 成功 | 查询成功、操作成功 |
| 201 | 创建成功 | 新增数据成功 |
| 400 | 请求错误 | 参数校验失败 |
| 404 | 未找到 | 实体不存在 |
| 500 | 服务器错误 | 系统异常 |
| 503 | 服务不可用 | 图数据库连接失败 |

---

## 2. API接口清单

### 2.1 模块划分

| 模块 | 前缀 | 说明 |
|------|------|------|
| 智能问答 | `/api/qa` | GraphRAG问答接口 |
| 实体管理 | `/api/entities` | 实体查询、详情 |
| 风险评估 | `/api/risk` | 风险计算、路径分析 |
| 图谱查询 | `/api/graph` | 子图、路径查询 |
| 数据接入 | `/api/ingestion` | 数据导入、快照管理 |
| 知识抽取 | `/api/extraction` | GSR-ER抽取任务 |
| 证据管理 | `/api/evidence` | 证据查询、追溯 |
| 系统管理 | `/api/system` | 统计、配置 |

### 2.2 核心接口清单

| 接口路径 | 方法 | 功能 | 优先级 |
|---------|------|------|--------|
| `/api/qa/ask` | POST | 智能问答 | P0 |
| `/api/entities/search` | GET | 实体搜索 | P0 |
| `/api/entities/{id}` | GET | 实体详情 | P0 |
| `/api/risk/calculate` | POST | 计算风险 | P0 |
| `/api/risk/{entityId}` | GET | 获取风险结果 | P0 |
| `/api/risk/paths/{entityId}` | GET | 获取风险路径 | P0 |
| `/api/graph/subgraph` | GET | 获取子图 | P1 |
| `/api/evidence/{id}` | GET | 获取证据详情 | P1 |
| `/api/ingestion/snapshots` | GET | 快照列表 | P1 |
| `/api/extraction/tasks` | POST | 创建抽取任务 | P1 |
| `/api/system/stats` | GET | 系统统计 | P1 |

---

## 3. 数据模型

### 3.1 MySQL 实体设计

#### 3.1.1 Snapshot (数据快照)
```java
@Entity
@Table(name = "snapshots")
public class Snapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String snapshotId;      // snap_20260205_ofac
    
    @Column(nullable = false)
    private String sourceName;       // OFAC SDN List
    
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;   // STRUCTURED_LIST, TEXT_CORPUS
    
    @Column(nullable = false)
    private LocalDateTime acquiredAt;
    
    private String sourceVersion;    // 2026-02-05
    private String fileHash;         // SHA-256
    private Integer recordCount;
    
    @Lob
    private String notes;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### 3.1.2 Evidence (证据)
```java
@Entity
@Table(name = "evidences")
public class Evidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String evidenceId;       // evd_gdelt_20260205_001
    
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;   // NEWS, SANCTION_LIST, GRAPH_ANALYSIS
    
    private String sourceRef;        // doc_gdelt_20260205_1847
    private String docId;
    
    @Lob
    private String textExcerpt;      // 原文片段
    
    private String span;             // 字符区间或句子ID
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private Double confidence;       // 0.0 - 1.0
    
    private String extractorVersion; // gsrer_v1.0
    
    @Lob
    private String metadata;         // JSON格式的额外元数据
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### 3.1.3 RiskResult (风险评估结果)
```java
@Entity
@Table(name = "risk_results")
public class RiskResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String entityId;         // Neo4j中的实体ID
    
    @Column(nullable = false)
    private Double riskScore;        // 0.0 - 1.0
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;     // HIGH, MEDIUM, LOW
    
    // 分量分值
    private Double pprScore;
    private Double communityScore;
    private Double hawkesScore;
    
    // 融合参数
    private Double alpha;
    private Double beta;
    private Double eta;
    
    private String modelVersion;     // hgt-ram_v1.0
    
    @Lob
    private String parametersJson;   // 完整参数配置
    
    @Column(nullable = false)
    private LocalDateTime computedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### 3.1.4 RiskPath (风险路径)
```java
@Entity
@Table(name = "risk_paths")
public class RiskPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "risk_result_id")
    private RiskResult riskResult;
    
    @Column(nullable = false)
    private Integer pathRank;        // 路径排名 1,2,3...
    
    @Column(nullable = false)
    private Double contribution;     // 贡献度 0.0 - 1.0
    
    @Lob
    @Column(nullable = false)
    private String pathJson;         // JSON格式的路径详情
    // [{"entityId": "ent_1", "entityName": "Viktor Petrov", "entityType": "Person"},
    //  {"edgeType": "financial_transaction"},
    //  {"entityId": "ent_2", "entityName": "ABC Trading", "entityType": "Organization"}]
    
    private String evidenceIds;      // 逗号分隔的证据ID列表
}
```

#### 3.1.5 ExtractionTask (抽取任务)
```java
@Entity
@Table(name = "extraction_tasks")
public class ExtractionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String taskId;           // task_20260206_001
    
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    private Snapshot snapshot;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;       // PENDING, RUNNING, COMPLETED, FAILED
    
    private Integer documentCount;
    private Integer entityCount;
    private Integer relationCount;
    private Integer eventCount;
    
    private Double validationPassRate; // 校验通过率
    
    @Lob
    private String configJson;       // 抽取配置参数
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    @Lob
    private String errorMessage;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 3.2 Neo4j 图数据模型

#### 3.2.1 Entity (节点)
```java
@NodeEntity
public class Entity {
    @Id
    @GeneratedValue
    private Long id;
    
    @Property(name = "entity_id")
    @Index(unique = true)
    private String entityId;         // ent_abc_trading
    
    @Property(name = "entity_type")
    private String entityType;       // Person, Organization
    
    @Property(name = "name")
    @Index
    private String name;             // ABC Trading Corp
    
    @Property(name = "aliases")
    private List<String> aliases;    // [ABC Trading, ABC Corp Ltd.]
    
    @Property(name = "identifiers")
    private Map<String, String> identifiers; // {registrationNo: "HK12345"}
    
    @Property(name = "addresses")
    private List<String> addresses;
    
    @Property(name = "status")
    private String status;           // active, deprecated
    
    @Property(name = "created_at")
    private LocalDateTime createdAt;
    
    @Property(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Property(name = "version")
    private Integer version;
    
    // 关系
    @Relationship(type = "RELATED_TO", direction = Relationship.OUTGOING)
    private List<EntityRelation> relations;
}
```

#### 3.2.2 Relation (关系)
```java
@RelationshipEntity(type = "RELATED_TO")
public class EntityRelation {
    @Id
    @GeneratedValue
    private Long id;
    
    @StartNode
    private Entity subject;
    
    @EndNode
    private Entity object;
    
    @Property(name = "predicate")
    private String predicate;        // financial_transaction, owns, member_of
    
    @Property(name = "weight")
    private Double weight;           // 边权重
    
    @Property(name = "first_seen_at")
    private LocalDateTime firstSeenAt;
    
    @Property(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
    
    @Property(name = "evidence_ids")
    private List<String> evidenceIds;
    
    @Property(name = "properties")
    private Map<String, Object> properties; // 额外属性如amount, time_period
}
```

---

## 4. 接口详细设计

### 4.1 智能问答模块

#### 4.1.1 POST /api/qa/ask

**功能**: 提交问题，返回证据化回答

**请求体**:
```json
{
  "question": "ABC Trading Corp 有哪些制裁风险？",
  "options": {
    "includeEvidence": true,
    "includePaths": true,
    "maxEvidences": 10
  }
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": {
      "summary": {
        "entityId": "ent_abc_trading",
        "entityName": "ABC Trading Corp",
        "riskLevel": "HIGH",
        "riskScore": 0.89,
        "recommendation": "不建议合作"
      },
      "content": "根据知识图谱分析，ABC Trading Corp 存在以下制裁关联风险...",
      "sections": [
        {
          "title": "与 OFAC 制裁个人直接交易",
          "content": "该实体与 Viktor Petrov（OFAC 2024年制裁）存在金融交易关系...",
          "evidenceRefs": ["evd_gdelt_20260205_001"]
        }
      ]
    },
    "evidences": [
      {
        "evidenceId": "evd_gdelt_20260205_001",
        "sourceType": "NEWS",
        "sourceRef": "GDELT News | 2026-02-05 14:22:00",
        "textExcerpt": "ABC Trading Corp, a Hong Kong-based entity...",
        "confidence": 0.92,
        "graphPath": "ent_viktor_petrov → financial_transaction → ent_abc_trading",
        "timestamp": "2026-02-05T14:22:00Z"
      }
    ],
    "paths": [
      {
        "pathRank": 1,
        "contribution": 0.34,
        "nodes": [
          {"entityId": "ent_viktor_petrov", "name": "Viktor Petrov", "type": "Person"},
          {"entityId": "ent_abc_trading", "name": "ABC Trading Corp", "type": "Organization"}
        ],
        "edges": [
          {"type": "financial_transaction", "properties": {"amount": "$2.3M"}}
        ]
      }
    ],
    "comparison": {
      "traditionalMethod": {
        "result": "未发现风险",
        "description": "仅能搜索到实体名称匹配的制裁清单条目"
      },
      "graphRAG": {
        "result": "HIGH 风险",
        "advantages": [
          "发现了2条间接风险传播路径",
          "整合了4个不同来源的证据",
          "识别了高风险社群关联"
        ]
      }
    }
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

**实现要点**:
```java
@RestController
@RequestMapping("/api/qa")
public class QAController {
    
    @Autowired
    private GraphRAGService graphRAGService;
    
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<QAResult>> ask(@RequestBody QARequest request) {
        // 1. 实体链接 (NER + Entity Linking)
        List<Entity> linkedEntities = graphRAGService.linkEntities(request.getQuestion());
        
        // 2. 图检索 (k-hop subgraph, paths, community members)
        GraphContext graphContext = graphRAGService.retrieveGraphContext(linkedEntities);
        
        // 3. 文本检索 (可选)
        List<Evidence> textEvidences = graphRAGService.retrieveTextEvidences(request.getQuestion());
        
        // 4. 证据拼接
        String evidencePackage = graphRAGService.buildEvidencePackage(graphContext, textEvidences);
        
        // 5. LLM生成回答
        String answer = graphRAGService.generateAnswer(request.getQuestion(), evidencePackage);
        
        // 6. 引用验证
        QAResult result = graphRAGService.validateAndBuildResult(answer, graphContext, textEvidences);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

---

### 4.2 实体管理模块

#### 4.2.1 GET /api/entities/search

**功能**: 实体搜索（支持名称、别名、ID）

**请求参数**:
```
?q=ABC Trading
&type=Organization
&page=0
&size=10
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "entityId": "ent_abc_trading",
        "entityType": "Organization",
        "name": "ABC Trading Corp",
        "aliases": ["ABC Trading", "ABC Corp Ltd."],
        "riskLevel": "HIGH",
        "riskScore": 0.89,
        "location": "Hong Kong",
        "updatedAt": "2026-02-05T16:30:05Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 10
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

**实现要点**:
```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<Page<EntityDTO>>> search(
    @RequestParam String q,
    @RequestParam(required = false) String type,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    // Neo4j Cypher查询
    // MATCH (e:Entity)
    // WHERE e.name CONTAINS $q OR ANY(alias IN e.aliases WHERE alias CONTAINS $q)
    // RETURN e
    // SKIP $skip LIMIT $limit
    
    Page<EntityDTO> results = entityService.search(q, type, PageRequest.of(page, size));
    return ResponseEntity.ok(ApiResponse.success(results));
}
```

#### 4.2.2 GET /api/entities/{id}

**功能**: 获取实体详情

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "entityId": "ent_abc_trading",
    "entityType": "Organization",
    "name": "ABC Trading Corp",
    "aliases": ["ABC Trading", "ABC Corp Ltd."],
    "identifiers": {
      "registrationNo": "HK12345"
    },
    "addresses": ["Hong Kong"],
    "status": "active",
    "version": 7,
    "createdAt": "2025-03-15T10:00:00Z",
    "updatedAt": "2026-02-05T16:30:05Z",
    "riskInfo": {
      "riskLevel": "HIGH",
      "riskScore": 0.89,
      "computedAt": "2026-02-06T10:15:23Z"
    },
    "relationCount": 8,
    "evidenceCount": 4
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

---

### 4.3 风险评估模块

#### 4.3.1 POST /api/risk/calculate

**功能**: 触发风险计算任务

**请求体**:
```json
{
  "entityIds": ["ent_abc_trading", "ent_global_ventures"],
  "config": {
    "pprDamping": 0.85,
    "leidenResolution": 1.0,
    "alpha": 0.5,
    "beta": 0.3,
    "eta": 0.2
  }
}
```

**响应**:
```json
{
  "code": 200,
  "message": "风险计算任务已提交",
  "data": {
    "taskId": "risk_task_20260206_001",
    "status": "RUNNING",
    "estimatedTime": "约2分钟"
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

**实现要点**:
```java
@PostMapping("/calculate")
public ResponseEntity<ApiResponse<TaskInfo>> calculate(@RequestBody RiskCalculateRequest request) {
    // 异步任务
    String taskId = riskService.submitCalculationTask(request);
    
    TaskInfo taskInfo = TaskInfo.builder()
        .taskId(taskId)
        .status("RUNNING")
        .estimatedTime("约2分钟")
        .build();
    
    return ResponseEntity.ok(ApiResponse.success(taskInfo));
}

// 异步任务实现
@Service
public class RiskService {
    
    @Async
    public String submitCalculationTask(RiskCalculateRequest request) {
        String taskId = generateTaskId();
        
        try {
            // 1. PPR计算 (Neo4j图算法库)
            Map<String, Double> pprScores = calculatePPR(request);
            
            // 2. 社群检测与风险评分
            Map<String, Double> communityScores = calculateCommunityRisk(request);
            
            // 3. Hawkes动态风险 (可选)
            Map<String, Double> hawkesScores = calculateHawkesRisk(request);
            
            // 4. 融合
            for (String entityId : request.getEntityIds()) {
                double riskScore = request.getAlpha() * pprScores.get(entityId)
                                 + request.getBeta() * communityScores.get(entityId)
                                 + request.getEta() * hawkesScores.getOrDefault(entityId, 0.0);
                
                // 5. 保存结果到MySQL
                saveRiskResult(entityId, riskScore, request);
            }
            
            // 6. 计算关键路径
            calculateRiskPaths(request.getEntityIds());
            
        } catch (Exception e) {
            log.error("Risk calculation failed for task: {}", taskId, e);
        }
        
        return taskId;
    }
}
```

#### 4.3.2 GET /api/risk/{entityId}

**功能**: 获取实体的风险评估结果

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "entityId": "ent_abc_trading",
    "riskScore": 0.89,
    "riskLevel": "HIGH",
    "components": {
      "ppr": 0.78,
      "community": 0.82,
      "hawkes": 0.65
    },
    "parameters": {
      "alpha": 0.5,
      "beta": 0.3,
      "eta": 0.2,
      "pprDamping": 0.85
    },
    "modelVersion": "hgt-ram_v1.0",
    "computedAt": "2026-02-06T10:15:23Z",
    "comparison": {
      "staticOnly": {
        "score": 0.78,
        "level": "MEDIUM"
      },
      "withCommunity": {
        "score": 0.80,
        "level": "MEDIUM"
      },
      "fullModel": {
        "score": 0.89,
        "level": "HIGH"
      }
    }
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

#### 4.3.3 GET /api/risk/paths/{entityId}

**功能**: 获取风险传播路径

**请求参数**:
```
?topK=3
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "entityId": "ent_abc_trading",
    "paths": [
      {
        "pathRank": 1,
        "contribution": 0.34,
        "nodes": [
          {
            "entityId": "ent_viktor_petrov",
            "name": "Viktor Petrov",
            "type": "Person",
            "riskLevel": "HIGH",
            "sanctionInfo": "OFAC 2024"
          },
          {
            "entityId": "ent_abc_trading",
            "name": "ABC Trading Corp",
            "type": "Organization"
          }
        ],
        "edges": [
          {
            "type": "financial_transaction",
            "properties": {
              "amount": "$2.3M",
              "timePeriod": "2025-01 to 2025-03"
            },
            "weight": 0.8
          }
        ],
        "evidenceIds": ["evd_gdelt_20260205_001"]
      }
    ]
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

**实现要点**:
```java
@GetMapping("/paths/{entityId}")
public ResponseEntity<ApiResponse<RiskPathsResult>> getPaths(
    @PathVariable String entityId,
    @RequestParam(defaultValue = "3") int topK
) {
    // 从MySQL查询已计算的路径
    List<RiskPath> paths = riskPathRepository.findTopKByEntityId(entityId, topK);
    
    // 补充路径详情（从Neo4j获取节点和边的完整信息）
    RiskPathsResult result = riskService.enrichPathDetails(paths);
    
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

---

### 4.4 图谱查询模块

#### 4.4.1 GET /api/graph/subgraph

**功能**: 获取k-hop子图

**请求参数**:
```
?entityId=ent_abc_trading
&hops=2
&maxNodes=50
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "center": {
      "entityId": "ent_abc_trading",
      "name": "ABC Trading Corp",
      "type": "Organization"
    },
    "nodes": [
      {
        "entityId": "ent_viktor_petrov",
        "name": "Viktor Petrov",
        "type": "Person",
        "distance": 1,
        "riskLevel": "HIGH"
      }
    ],
    "edges": [
      {
        "source": "ent_viktor_petrov",
        "target": "ent_abc_trading",
        "type": "financial_transaction",
        "weight": 0.8,
        "properties": {"amount": "$2.3M"}
      }
    ],
    "statistics": {
      "nodeCount": 12,
      "edgeCount": 18,
      "avgDistance": 1.5
    }
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

**实现要点**:
```java
@GetMapping("/subgraph")
public ResponseEntity<ApiResponse<SubgraphResult>> getSubgraph(
    @RequestParam String entityId,
    @RequestParam(defaultValue = "2") int hops,
    @RequestParam(defaultValue = "50") int maxNodes
) {
    // Neo4j Cypher查询
    // MATCH path = (center:Entity {entity_id: $entityId})-[*1..${hops}]-(neighbor)
    // RETURN center, neighbor, relationships(path)
    // LIMIT $maxNodes
    
    SubgraphResult subgraph = graphService.getSubgraph(entityId, hops, maxNodes);
    return ResponseEntity.ok(ApiResponse.success(subgraph));
}
```

---

### 4.5 数据接入模块

#### 4.5.1 GET /api/ingestion/snapshots

**功能**: 获取快照列表

**请求参数**:
```
?sourceType=STRUCTURED_LIST
&page=0
&size=20
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "snapshotId": "snap_20260205_ofac",
        "sourceName": "OFAC SDN List",
        "sourceType": "STRUCTURED_LIST",
        "sourceVersion": "2026-02-05",
        "recordCount": 8472,
        "fileHash": "a8f3e2d1c9b7f6e4a3d2c1b0a9f8e7d6",
        "acquiredAt": "2026-02-05T16:30:05Z",
        "qualityMetrics": {
          "parseSuccessRate": 0.998,
          "duplicateRate": 0.023,
          "missingFieldsRate": 0.005
        }
      }
    ],
    "totalElements": 4,
    "totalPages": 1
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

#### 4.5.2 POST /api/ingestion/snapshots

**功能**: 创建新快照导入任务

**请求体** (multipart/form-data):
```
sourceName: OFAC SDN List
sourceType: STRUCTURED_LIST
sourceVersion: 2026-02-10
file: [文件上传]
```

**响应**:
```json
{
  "code": 201,
  "message": "快照导入任务已创建",
  "data": {
    "snapshotId": "snap_20260210_ofac",
    "taskId": "import_task_20260210_001",
    "status": "PROCESSING"
  },
  "timestamp": "2026-02-10T09:00:00Z"
}
```

---

### 4.6 知识抽取模块

#### 4.6.1 POST /api/extraction/tasks

**功能**: 创建知识抽取任务

**请求体**:
```json
{
  "snapshotId": "snap_20260206_gdelt",
  "config": {
    "modelName": "claude-sonnet-4-5",
    "temperature": 0.7,
    "maxTokens": 4000,
    "kHop": 2,
    "enableSelfRefine": true,
    "maxRefineRounds": 3
  }
}
```

**响应**:
```json
{
  "code": 201,
  "message": "抽取任务已创建",
  "data": {
    "taskId": "task_20260206_001",
    "status": "PENDING",
    "estimatedDocuments": 234
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

#### 4.6.2 GET /api/extraction/tasks/{taskId}

**功能**: 获取抽取任务状态

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260206_001",
    "status": "COMPLETED",
    "snapshot": {
      "snapshotId": "snap_20260206_gdelt",
      "sourceName": "GDELT News"
    },
    "progress": {
      "documentCount": 234,
      "processedCount": 234,
      "percentage": 100
    },
    "results": {
      "entityCount": 1284,
      "relationCount": 2847,
      "eventCount": 423,
      "validationPassRate": 0.973
    },
    "comparison": {
      "baselineNER": {
        "entityCount": 1047,
        "relationCount": 1823,
        "accuracy": 0.892
      },
      "withGraphContext": {
        "entityCount": 1198,
        "relationCount": 2456,
        "accuracy": 0.941
      },
      "gsrER": {
        "entityCount": 1284,
        "relationCount": 2847,
        "accuracy": 0.973
      }
    },
    "startedAt": "2026-02-06T08:42:11Z",
    "completedAt": "2026-02-06T09:00:43Z",
    "duration": "18m32s"
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

---

### 4.7 证据管理模块

#### 4.7.1 GET /api/evidence/{id}

**功能**: 获取证据详情

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "evidenceId": "evd_gdelt_20260205_001",
    "sourceType": "NEWS",
    "sourceRef": "GDELT News | 2026-02-05 14:22:00",
    "docId": "doc_gdelt_20260205_1847",
    "textExcerpt": "ABC Trading Corp, a Hong Kong-based entity, has been identified...",
    "span": "chars:245-512",
    "timestamp": "2026-02-05T14:22:00Z",
    "confidence": 0.92,
    "extractorVersion": "gsrer_v1.0",
    "relatedFacts": [
      {
        "factType": "relation",
        "subject": "ent_abc_trading",
        "predicate": "financial_transaction",
        "object": "ent_viktor_petrov"
      }
    ],
    "metadata": {
      "publisher": "GDELT",
      "language": "en",
      "url": "https://..."
    }
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

---

### 4.8 系统管理模块

#### 4.8.1 GET /api/system/stats

**功能**: 获取系统统计信息

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "graph": {
      "entityCount": 12847,
      "relationCount": 28392,
      "entityTypes": {
        "Person": 4523,
        "Organization": 7834,
        "Location": 490
      }
    },
    "risk": {
      "highRiskCount": 1247,
      "mediumRiskCount": 3842,
      "lowRiskCount": 7758
    },
    "evidence": {
      "totalCount": 45628,
      "bySourceType": {
        "NEWS": 32145,
        "SANCTION_LIST": 8472,
        "GRAPH_ANALYSIS": 5011
      }
    },
    "updates": {
      "lastSnapshotAt": "2026-02-06T08:00:12Z",
      "lastExtractionAt": "2026-02-06T08:42:11Z",
      "lastRiskCalcAt": "2026-02-06T10:15:23Z"
    },
    "weeklyChanges": {
      "entityChange": 384,
      "relationChange": 892,
      "highRiskChange": 23
    }
  },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

---

## 5. 数据库设计

### 5.1 MySQL表设计

```sql
-- 快照表
CREATE TABLE snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id VARCHAR(100) UNIQUE NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    source_type ENUM('STRUCTURED_LIST', 'TEXT_CORPUS') NOT NULL,
    acquired_at DATETIME NOT NULL,
    source_version VARCHAR(50),
    file_hash VARCHAR(64),
    record_count INT,
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_type (source_type),
    INDEX idx_acquired_at (acquired_at)
);

-- 证据表
CREATE TABLE evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evidence_id VARCHAR(100) UNIQUE NOT NULL,
    source_type ENUM('NEWS', 'SANCTION_LIST', 'GRAPH_ANALYSIS') NOT NULL,
    source_ref VARCHAR(500),
    doc_id VARCHAR(100),
    text_excerpt TEXT,
    span VARCHAR(100),
    timestamp DATETIME,
    confidence DECIMAL(5,4) NOT NULL,
    extractor_version VARCHAR(50),
    metadata JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_type (source_type),
    INDEX idx_timestamp (timestamp),
    FULLTEXT idx_text_excerpt (text_excerpt)
);

-- 风险结果表
CREATE TABLE risk_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_id VARCHAR(100) NOT NULL,
    risk_score DECIMAL(5,4) NOT NULL,
    risk_level ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL,
    ppr_score DECIMAL(5,4),
    community_score DECIMAL(5,4),
    hawkes_score DECIMAL(5,4),
    alpha DECIMAL(3,2),
    beta DECIMAL(3,2),
    eta DECIMAL(3,2),
    model_version VARCHAR(50),
    parameters_json JSON,
    computed_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity_id (entity_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_computed_at (computed_at)
);

-- 风险路径表
CREATE TABLE risk_paths (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_result_id BIGINT NOT NULL,
    path_rank INT NOT NULL,
    contribution DECIMAL(5,4) NOT NULL,
    path_json JSON NOT NULL,
    evidence_ids TEXT,
    FOREIGN KEY (risk_result_id) REFERENCES risk_results(id) ON DELETE CASCADE,
    INDEX idx_risk_result (risk_result_id)
);

-- 抽取任务表
CREATE TABLE extraction_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) UNIQUE NOT NULL,
    snapshot_id BIGINT,
    status ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED') NOT NULL,
    document_count INT,
    entity_count INT,
    relation_count INT,
    event_count INT,
    validation_pass_rate DECIMAL(5,4),
    config_json JSON,
    started_at DATETIME,
    completed_at DATETIME,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (snapshot_id) REFERENCES snapshots(id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### 5.2 Neo4j约束与索引

```cypher
// 创建唯一约束
CREATE CONSTRAINT entity_id_unique IF NOT EXISTS
FOR (e:Entity) REQUIRE e.entity_id IS UNIQUE;

// 创建索引
CREATE INDEX entity_name_idx IF NOT EXISTS
FOR (e:Entity) ON (e.name);

CREATE INDEX entity_type_idx IF NOT EXISTS
FOR (e:Entity) ON (e.entity_type);

CREATE INDEX entity_status_idx IF NOT EXISTS
FOR (e:Entity) ON (e.status);

// 关系索引
CREATE INDEX relation_predicate_idx IF NOT EXISTS
FOR ()-[r:RELATED_TO]-() ON (r.predicate);
```

---

## 6. 技术实现建议

### 6.1 项目结构

```
sanction-kg-backend/
├── src/main/java/com/sanction/kg/
│   ├── SanctionKgApplication.java
│   ├── config/
│   │   ├── Neo4jConfig.java
│   │   ├── MySQLConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── AsyncConfig.java
│   ├── controller/
│   │   ├── QAController.java
│   │   ├── EntityController.java
│   │   ├── RiskController.java
│   │   ├── GraphController.java
│   │   ├── IngestionController.java
│   │   ├── ExtractionController.java
│   │   └── SystemController.java
│   ├── service/
│   │   ├── GraphRAGService.java
│   │   ├── EntityService.java
│   │   ├── RiskService.java
│   │   ├── GraphService.java
│   │   └── ExtractionService.java
│   ├── repository/
│   │   ├── mysql/
│   │   │   ├── SnapshotRepository.java
│   │   │   ├── EvidenceRepository.java
│   │   │   ├── RiskResultRepository.java
│   │   │   └── RiskPathRepository.java
│   │   └── neo4j/
│   │       ├── EntityRepository.java
│   │       └── EntityRelationRepository.java
│   ├── entity/
│   │   ├── mysql/
│   │   │   ├── Snapshot.java
│   │   │   ├── Evidence.java
│   │   │   ├── RiskResult.java
│   │   │   └── RiskPath.java
│   │   └── neo4j/
│   │       ├── Entity.java
│   │       └── EntityRelation.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── QARequest.java
│   │   │   ├── RiskCalculateRequest.java
│   │   │   └── ExtractionTaskRequest.java
│   │   └── response/
│   │       ├── QAResult.java
│   │       ├── EntityDTO.java
│   │       └── RiskPathsResult.java
│   └── common/
│       ├── ApiResponse.java
│       ├── GlobalExceptionHandler.java
│       └── enums/
│           ├── RiskLevel.java
│           ├── SourceType.java
│           └── TaskStatus.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
└── pom.xml
```

### 6.2 核心依赖 (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA (MySQL) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- Spring Data Neo4j -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-neo4j</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Swagger/OpenAPI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-ui</artifactId>
        <version>1.7.0</version>
    </dependency>
    
    <!-- Redis (可选) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Async/Task -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>
</dependencies>
```

### 6.3 配置文件 (application.yml)

```yaml
spring:
  application:
    name: sanction-kg-backend
  
  # MySQL配置
  datasource:
    url: jdbc:mysql://localhost:3306/sanction_kg?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Neo4j配置
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your_password
  
  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api

# 自定义配置
sanction-kg:
  # LLM配置
  llm:
    provider: anthropic
    model: claude-sonnet-4-5
    api-key: ${ANTHROPIC_API_KEY}
  
  # 风险评估默认参数
  risk:
    default-alpha: 0.5
    default-beta: 0.3
    default-eta: 0.2
    ppr-damping: 0.85
    leiden-resolution: 1.0
  
  # 异步任务配置
  async:
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 100

# Swagger配置
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### 6.4 关键Service实现示例

#### RiskService核心逻辑

```java
@Service
@Slf4j
public class RiskService {
    
    @Autowired
    private EntityRepository entityRepository;
    
    @Autowired
    private RiskResultRepository riskResultRepository;
    
    @Autowired
    private RiskPathRepository riskPathRepository;
    
    @Autowired
    private Neo4jClient neo4jClient;
    
    @Value("${sanction-kg.risk.default-alpha}")
    private Double defaultAlpha;
    
    @Value("${sanction-kg.risk.default-beta}")
    private Double defaultBeta;
    
    @Value("${sanction-kg.risk.default-eta}")
    private Double defaultEta;
    
    /**
     * 计算PPR分值
     */
    private Map<String, Double> calculatePPR(RiskCalculateRequest request) {
        // 使用Neo4j GDS (Graph Data Science)库
        String cypher = """
            CALL gds.pageRank.stream({
                nodeProjection: 'Entity',
                relationshipProjection: {
                    RELATED_TO: {
                        type: 'RELATED_TO',
                        properties: 'weight',
                        orientation: 'NATURAL'
                    }
                },
                dampingFactor: $dampingFactor,
                sourceNodes: $sourceNodes
            })
            YIELD nodeId, score
            RETURN gds.util.asNode(nodeId).entity_id AS entityId, score
            """;
        
        // 获取高风险种子节点（已制裁实体）
        List<String> sourceNodes = getHighRiskSeedNodes();
        
        Map<String, Object> params = Map.of(
            "dampingFactor", request.getPprDamping(),
            "sourceNodes", sourceNodes
        );
        
        Collection<Map<String, Object>> results = neo4jClient
            .query(cypher)
            .bindAll(params)
            .fetch()
            .all();
        
        return results.stream()
            .collect(Collectors.toMap(
                r -> (String) r.get("entityId"),
                r -> (Double) r.get("score")
            ));
    }
    
    /**
     * 计算社群风险
     */
    private Map<String, Double> calculateCommunityRisk(RiskCalculateRequest request) {
        // 1. 运行Leiden社群检测
        String leidenCypher = """
            CALL gds.leiden.write({
                nodeProjection: 'Entity',
                relationshipProjection: 'RELATED_TO',
                writeProperty: 'community',
                resolution: $resolution
            })
            YIELD communityCount
            RETURN communityCount
            """;
        
        neo4jClient
            .query(leidenCypher)
            .bind(request.getLeidenResolution()).to("resolution")
            .fetch()
            .one();
        
        // 2. 计算每个社群的风险分值
        String commRiskCypher = """
            MATCH (e:Entity)
            WITH e.community AS commId, 
                 COUNT(e) AS totalMembers,
                 SUM(CASE WHEN e.sanction_status IS NOT NULL THEN 1 ELSE 0 END) AS sanctionedMembers
            WITH commId, 
                 totalMembers,
                 sanctionedMembers,
                 1.0 * sanctionedMembers / totalMembers AS commRisk
            MATCH (entity:Entity {community: commId})
            RETURN entity.entity_id AS entityId, commRisk
            """;
        
        Collection<Map<String, Object>> results = neo4jClient
            .query(commRiskCypher)
            .fetch()
            .all();
        
        return results.stream()
            .collect(Collectors.toMap(
                r -> (String) r.get("entityId"),
                r -> (Double) r.get("commRisk")
            ));
    }
    
    /**
     * 计算Hawkes动态风险（简化版）
     */
    private Map<String, Double> calculateHawkesRisk(RiskCalculateRequest request) {
        // 实际实现需要：
        // 1. 从evidence表查询与实体相关的时序事件
        // 2. 计算Hawkes过程强度
        // 3. 这里用简化逻辑：基于近期事件频率
        
        String eventCountCypher = """
            MATCH (e:Entity)-[:HAS_EVIDENCE]->(ev:Evidence)
            WHERE ev.timestamp > datetime() - duration({days: 90})
            WITH e.entity_id AS entityId, COUNT(ev) AS recentEvents
            RETURN entityId, 
                   CASE 
                     WHEN recentEvents > 10 THEN 0.8
                     WHEN recentEvents > 5 THEN 0.5
                     ELSE 0.2
                   END AS hawkesScore
            """;
        
        Collection<Map<String, Object>> results = neo4jClient
            .query(eventCountCypher)
            .fetch()
            .all();
        
        return results.stream()
            .collect(Collectors.toMap(
                r -> (String) r.get("entityId"),
                r -> (Double) r.get("hawkesScore")
            ));
    }
    
    /**
     * 计算关键路径
     */
    private void calculateRiskPaths(List<String> entityIds) {
        for (String entityId : entityIds) {
            // Cypher查询：从高风险节点到目标节点的最短加权路径
            String pathCypher = """
                MATCH (source:Entity)
                WHERE source.sanction_status IS NOT NULL
                MATCH (target:Entity {entity_id: $entityId})
                MATCH path = shortestPath((source)-[:RELATED_TO*1..4]-(target))
                WITH path, 
                     reduce(weight = 0.0, r IN relationships(path) | weight + r.weight) AS totalWeight
                ORDER BY totalWeight DESC
                LIMIT 5
                RETURN [node IN nodes(path) | {
                    entityId: node.entity_id,
                    name: node.name,
                    type: node.entity_type
                }] AS nodes,
                [rel IN relationships(path) | {
                    type: type(rel),
                    weight: rel.weight,
                    properties: properties(rel)
                }] AS edges,
                totalWeight
                """;
            
            Collection<Map<String, Object>> pathResults = neo4jClient
                .query(pathCypher)
                .bind(entityId).to("entityId")
                .fetch()
                .all();
            
            // 保存路径到MySQL
            RiskResult riskResult = riskResultRepository.findByEntityId(entityId);
            int rank = 1;
            for (Map<String, Object> pathData : pathResults) {
                RiskPath riskPath = new RiskPath();
                riskPath.setRiskResult(riskResult);
                riskPath.setPathRank(rank++);
                riskPath.setContribution((Double) pathData.get("totalWeight") / 10.0); // 归一化
                riskPath.setPathJson(new ObjectMapper().writeValueAsString(pathData));
                
                riskPathRepository.save(riskPath);
            }
        }
    }
}
```

---

## 7. 部署与运维

### 7.1 开发环境运行

```bash
# 1. 启动MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=sanction_kg \
  -p 3306:3306 \
  mysql:8.0

# 2. 启动Neo4j
docker run -d --name neo4j \
  -e NEO4J_AUTH=neo4j/neo4j123 \
  -p 7474:7474 -p 7687:7687 \
  neo4j:4.4

# 3. 运行Spring Boot应用
./mvnw spring-boot:run
```

### 7.2 生产环境建议

- **容器化**: 使用Docker + Docker Compose部署
- **负载均衡**: Nginx反向代理多个Spring Boot实例
- **数据库**: MySQL主从复制 + Neo4j企业版集群
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack (Elasticsearch + Logback + Kibana)

---

## 附录A: 接口调用示例

### 示例1: 完整问答流程

```bash
# 1. 提问
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "ABC Trading Corp 有风险吗？",
    "options": {
      "includeEvidence": true,
      "includePaths": true
    }
  }'

# 2. 查看实体详情
curl http://localhost:8080/api/entities/ent_abc_trading

# 3. 获取风险路径
curl http://localhost:8080/api/risk/paths/ent_abc_trading?topK=3

# 4. 获取证据
curl http://localhost:8080/api/evidence/evd_gdelt_20260205_001
```

---

**文档维护**: 
- 初始版本: 2026-02-06
- 联系人: 后端开发团队
- 文档状态: 设计阶段
