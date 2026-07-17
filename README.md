# Flowable 多机构工作流引擎服务

## 快速启动

### 1. 环境准备

```bash
# 确保 MySQL 运行中
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS flowable_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 安装 Maven
mvn -v
```

### 2. 编译打包

```bash
cd /root/flowable-engine-service
mvn clean package -DskipTests
```

### 3. 启动服务

```bash
java -jar target/flowable-engine-service-1.0.0-SNAPSHOT.jar
```

服务默认运行在 `http://localhost:9999`

---

## API 文档

### 基础信息
- **Base URL**: `http://localhost:9999/api`
- **租户 Header**: `X-Tenant-Id: ORG_TENANT_XXX`
- **用户 Header**: `X-User-Id: zhangsan`

### 1. 流程定义管理

#### 部署流程定义
```bash
curl -X POST http://localhost:9999/api/process/definition/deploy \
  -F "file=@src/main/resources/processes/leave.bpmn20.xml" \
  -F "tenantId=ORG_TENANT_001"
```

#### 查询流程定义列表
```bash
curl "http://localhost:9999/api/process/definition/list?key=leave_request&page=1&size=20"
```

#### 挂起/激活流程定义
```bash
curl -X POST http://localhost:9999/api/process/definition/{definitionId}/suspend
curl -X POST http://localhost:9999/api/process/definition/{definitionId}/activate
```

### 2. 流程实例管理

#### 启动流程实例
```bash
curl -X POST http://localhost:9999/api/process/instance/start \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: ORG_TENANT_001" \
  -d '{
    "processDefinitionKey": "leave_request",
    "businessKey": "ORDER_20260716001",
    "variables": {
      "applicant": "zhangsan",
      "manager": "lisi",
      "days": 2
    }
  }'
```

#### 查询我的实例
```bash
curl "http://localhost:9999/api/process/instance/list?businessKey=ORDER_001&page=1&size=20" \
  -H "X-Tenant-Id: ORG_TENANT_001"
```

### 3. 任务管理

#### 查询待办
```bash
curl "http://localhost:9999/api/task/todo?assignee=lisi&page=1&size=50" \
  -H "X-Tenant-Id: ORG_TENANT_001"
```

#### 完成任务
```bash
curl -X POST http://localhost:9999/api/task/complete \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "<taskId>",
    "variables": {"approved": true},
    "comment": "同意"
  }'
```

#### 签收/转派
```bash
curl -X POST http://localhost:9999/api/task/claim \
  -H "Content-Type: application/json" \
  -d '{"taskId": "<taskId>", "assignee": "lisi"}'

curl -X POST http://localhost:9999/api/task/delegate \
  -H "Content-Type: application/json" \
  -d '{"taskId": "<taskId>", "newAssignee": "wangwu"}'
```

### 4. 历史追溯

#### 历史流程实例
```bash
curl "http://localhost:9999/api/history/processes?businessKey=ORDER_001" \
  -H "X-Tenant-Id: ORG_TENANT_001"
```

#### 历史任务记录
```bash
curl "http://localhost:9999/api/history/tasks?processInstanceId=<processInstanceId>"
```

---

## 多机构权限说明

| 用户身份 | X-Tenant-Id | 能看的流程范围 |
|----------|-------------|----------------|
| 总公司总经理 | ORG_TENANT_001 | ORG_TENANT_001~005（全部） |
| 华东分公司经理 | ORG_TENANT_002 | ORG_TENANT_002、003（自己和下属） |
| 上海办事处主任 | ORG_TENANT_003 | 仅 ORG_TENANT_003 |
| 深圳办事员工 | ORG_TENANT_005 | 仅 ORG_TENANT_005 |

**实现原理：** 每次查询自动注入 `tenantId IN (当前 + 所有子孙节点)`，通过 MyBatis 递归 CTE 查询机构树。

---

## 项目结构

```
flowable-engine-service/
├── pom.xml                          # Maven 依赖
├── src/main/java/com/example/workflow/
│   ├── WorkflowApplication.java     # Spring Boot 入口
│   ├── controller/                  # REST API
│   │   ├── ProcessDefinitionController.java
│   │   ├── ProcessInstanceController.java
│   │   ├── TaskController.java
│   │   └── HistoricController.java
│   ├── service/                     # 业务逻辑
│   │   ├── OrgService.java          # 机构树查询
│   │   ├── DeploymentService.java   # 流程部署
│   │   └── FlowableQueryHelper.java # 权限拦截器
│   ├── dto/                         # 请求/响应对象
│   └── security/                    # 安全工具类
├── src/main/resources/
│   ├── application.yml              # 配置文件
│   ├── processes/                   # BPMN 流程定义
│   └── db/migration/                # Flyway 迁移脚本
└── src/test/java/                   # 集成测试
```
