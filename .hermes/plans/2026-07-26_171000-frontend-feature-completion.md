# 流程审批系统 - 前端功能补全计划书

> **For Hermes:** 按任务顺序依次执行，每个任务完成后 git commit + push。

**Goal:** 补齐后端已有但前端缺失的 50 个 API 对接，覆盖流程管理、审批操作、表单设计、模板管理等核心功能。

**Architecture:** 基于现有 React 18 + Vite + Ant Design 5 + Zustand 架构，新增/完善前端页面和 API 层。

**Tech Stack:** React 18, Ant Design 5, Vite, Zustand, Axios, React Router v6

---

## 当前状态

### ✅ 已覆盖（18个 API）
- Auth: 登录、用户CRUD、机构CRUD、角色CRUD
- Workflow: 启动流程、提交审批、待办查询
- Form: 表单列表查询
- Monitor: 监控看板、实例列表、轨迹、活跃节点

### ❌ 缺失（50个 API）
| 模块 | 缺失数 | 优先级 |
|------|--------|--------|
| Task (待办/已办) | 7 | 🔴 P0 |
| Workflow (驳回/终止/详情) | 9 | 🔴 P0 |
| ProcessDefinition (部署/挂起) | 5 | 🔴 P0 |
| ApprovalTemplate (审批模板) | 5 | 🟡 P1 |
| Form (表单Schema CRUD) | 7 | 🟡 P1 |
| Historic (历史查询) | 2 | 🟡 P1 |
| Committee (委员会投票) | 4 | 🟢 P2 |
| Escalation (超时升级) | 2 | 🟢 P2 |
| UserLifecycle (调岗/辞职) | 3 | 🟢 P2 |

---

## 任务清单

### Task 1: 修复 Task API 层 — 待办/已办/完成/驳回/转交/委托
**Objective:** 创建完整的 task API 文件，覆盖所有待办任务操作。

**Files:**
- Create: `frontend/src/api/task.ts`
- Modify: `frontend/src/pages/TaskTodo/index.tsx`

**Step 1: 查看后端 TaskController 完整接口**

```bash
grep -n "@.*Mapping\|@.*Request" /root/flowable-engine-service/src/main/java/com/example/workflow/controller/TaskController.java
```

**Step 2: 读取 TaskController 完整代码**

```bash
cat /root/flowable-engine-service/src/main/java/com/example/workflow/controller/TaskController.java
```

**Step 3: 创建 task.ts API 文件**

参考现有 `frontend/src/api/auth.ts` 格式，创建：
```typescript
import request from '@/utils/request';

// 我的待办
export const getTodoTasks = (params?: any) => request.get('/workflow/todo', { params });
// 我的已办
export const getDoneTasks = (params?: any) => request.get('/workflow/done', { params });
// 完成任务
export const completeTask = (data: any) => request.post('/workflow/submit', data);
// 驳回到指定节点
export const rejectToNode = (data: any) => request.post('/workflow/reject', data);
// 驳回到开始
export const rejectToStart = (data: any) => request.post('/workflow/reject-to-start', data);
// 转交任务
export const delegateTask = (taskId: string, assignee: string, comment?: string) => 
  request.post(`/wf/task/delegate/${taskId}`, { assignee, comment });
// 认领任务
export const claimTask = (taskId: string, userId: string) => 
  request.post(`/wf/task/claim/${taskId}`, { userId });
// 分配任务
export const assignTask = (taskId: string, userId: string) => 
  request.post(`/wf/task/assign/${taskId}`, { userId });
```

**Step 4: 验证 API 文件语法**
```bash
cd /root/flowable-engine-service/frontend && npx tsc --noEmit src/api/task.ts 2>&1 | head -5
```

**Step 5: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/api/task.ts && git commit -m "feat: 新增任务管理 API 层" && git push
```

---

### Task 2: 完善 TaskTodo 页面 — 添加已完成/驳回/转交/委托操作
**Objective:** 在现有待办页面中添加任务操作按钮和弹窗。

**Files:**
- Modify: `frontend/src/pages/TaskTodo/index.tsx`

**Step 1: 读取现有 TaskTodo 页面**

```bash
cat /root/flowable-engine-service/frontend/src/pages/TaskTodo/index.tsx
```

**Step 2: 增强表格列 — 添加操作列**

在现有表格 columns 中添加：
```tsx
{
  title: '操作',
  key: 'actions',
  width: 200,
  render: (_: any, record: any) => (
    <Space>
      <Button type="primary" size="small" onClick={() => handleComplete(record)}>通过</Button>
      <Button size="small" onClick={() => setShowRejectModal(record)}>驳回</Button>
      <Dropdown menu={{ items: [
        { key: 'delegate', label: '转交', onClick: () => handleDelegate(record) },
        { key: 'claim', label: '认领', onClick: () => handleClaim(record) },
      ]}}>
        <Button size="small">更多</Button>
      </Dropdown>
    </Space>
  ),
}
```

**Step 3: 添加 Modal 组件**

添加驳回确认弹窗和转交流程弹窗（使用 Ant Design Modal + Form）。

**Step 4: 实现处理函数**

```tsx
const handleComplete = async (task: any) => {
  try {
    await completeTask({ taskId: task.taskId, variables: { approved: true } });
    message.success('审批通过');
    loadTasks(); // 刷新列表
  } catch (e) {
    message.error('操作失败');
  }
};
```

**Step 5: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/TaskTodo/index.tsx && git commit -m "feat: 完善待办任务页面 — 支持通过/驳回/转交" && git push
```

---

### Task 3: 新增 Workflow 管理页面 — 流程实例详情/轨迹/终止/挂起
**Objective:** 从监控页面分离出独立的流程管理页面，支持对单个流程实例的深度操作。

**Files:**
- Create: `frontend/src/pages/WorkflowManage/index.tsx`
- Create: `frontend/src/api/workflow.ts` (补充现有缺失方法)

**Step 1: 扩展 workflow.ts API**

```typescript
// 获取流程实例详情
export const getInstanceDetail = (id: string) => request.get(`/workflow/instance/${id}`);
// 获取流程轨迹
export const getProcessTrace = (id: string) => request.get(`/workflow/trace/${id}`);
// 获取活跃节点
export const getActiveNodes = (id: string) => request.get(`/workflow/active-nodes/${id}`);
// 终止流程
export const terminateProcess = (id: string, reason?: string) => 
  request.post(`/workflow/terminate/${id}`, { reason: reason || '用户主动终止' });
// 挂起流程
export const suspendProcess = (id: string) => request.post(`/workflow/suspend/${id}`);
// 激活流程
export const activateProcess = (id: string) => request.post(`/workflow/activate/${id}`);
```

**Step 2: 创建 WorkflowManage 页面**

包含：
- 流程实例列表（复用 monitor 的实例列表组件）
- 点击某实例后展开详情面板（显示变量、开始时间等）
- 轨迹时间线（复用 monitor 的 trace 组件）
- 操作按钮组（终止/挂起/激活）

**Step 3: 在 App.tsx 添加路由**
```tsx
<Route path="/workflow-manage" element={<WorkflowManage />} />
```

**Step 4: 在 MainLayout 菜单添加入口**
```tsx
{ key: '/workflow-manage', icon: <FileTextOutlined />, label: '流程管理' },
```

**Step 5: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/WorkflowManage/ frontend/src/api/workflow.ts frontend/src/App.tsx frontend/src/components/Layout/MainLayout.tsx && git commit -m "feat: 新增流程管理页面 — 实例详情/轨迹/终止/挂起" && git push
```

---

### Task 4: 新增 ProcessDefinition 管理页面 — 部署/列表/挂起/激活
**Objective:** 让管理员可以上传 BPMN XML、管理流程定义版本。

**Files:**
- Create: `frontend/src/pages/ProcessDefManage/index.tsx`
- Create: `frontend/src/api/processDef.ts`

**Step 1: 查看后端 ProcessDefinitionController**

```bash
cat /root/flowable-engine-service/src/main/java/com/example/workflow/controller/ProcessDefinitionController.java
```

**Step 2: 创建 processDef.ts API**

```typescript
// 部署流程定义（上传 BPMN XML）
export const deployProcess = (formData: FormData) => 
  request.post('/process/definition/deploy', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
// 流程定义列表
export const getProcessDefList = () => request.get('/process/definition/list');
// 挂起流程定义
export const suspendProcessDef = (id: string) => request.post(`/process/definition/${id}/suspend`);
// 激活流程定义
export const activateProcessDef = (id: string) => request.post(`/process/definition/${id}/activate`);
// 删除流程定义
export const deleteProcessDef = (deploymentId: string) => request.delete(`/process/definition/${deploymentId}`);
```

**Step 3: 创建 ProcessDefManage 页面**

- 文件上传区域（拖拽上传 BPMN XML）
- 流程定义列表表格（名称、Key、版本、状态、操作）
- 操作按钮：挂起/激活/删除

**Step 4: 添加路由和菜单**

**Step 5: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/ProcessDefManage/ frontend/src/api/processDef.ts && git commit -m "feat: 新增流程定义管理页面 — 部署/挂起/激活" && git push
```

---

### Task 5: 新增 ApprovalTemplate 管理页面 — 审批模板 CRUD
**Objective:** 管理审批规则模板（逐级/固定节点/动态规则）。

**Files:**
- Create: `frontend/src/pages/TemplateManage/index.tsx`
- Create: `frontend/src/api/template.ts`

**Step 1: 查看后端 ApprovalTemplateController**

```bash
cat /root/flowable-engine-service/src/main/java/com/example/workflow/controller/ApprovalTemplateController.java
```

**Step 2: 创建 template.ts API**

**Step 3: 创建 TemplateManage 页面**

- 模板列表（名称、关联流程、规则类型、状态）
- 新增/编辑弹窗（JSON 编辑器配置 rule_config）
- 启用/禁用切换开关

**Step 4: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/TemplateManage/ frontend/src/api/template.ts && git commit -m "feat: 新增审批模板管理页面" && git push
```

---

### Task 6: 完善 Form Schema 管理页面 — 创建/编辑/删除表单
**Objective:** 让管理员可以设计和管理流程表单 Schema。

**Files:**
- Create: `frontend/src/pages/FormDesign/index.tsx`
- Modify: `frontend/src/api/form.ts`

**Step 1: 扩展 form.ts API**

**Step 2: 创建 FormDesign 页面**

- 表单列表
- JSON Schema 编辑器（使用 antd 的 Input.TextArea 或 monaco-editor）
- UI Schema 预览

**Step 3: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/FormDesign/ frontend/src/api/form.ts && git commit -m "feat: 完善表单Schema设计页面" && git push
```

---

### Task 7: 新增 Historic 历史查询页面
**Objective:** 查看已完成的历史流程和任务记录。

**Files:**
- Create: `frontend/src/pages/HistoricQuery/index.tsx`
- Create: `frontend/src/api/historic.ts`

**Step 1: 创建 historic.ts API**

**Step 2: 创建 HistoricQuery 页面**

- 筛选条件：流程类型、时间范围、业务单号
- 历史记录表格（含耗时、完成时间）
- 点击可查看详情和轨迹

**Step 3: Commit**
```bash
cd /root/flowable-engine-service && git add frontend/src/pages/HistoricQuery/ frontend/src/api/historic.ts && git commit -m "feat: 新增历史查询页面" && git push
```

---

### Task 8: 新增 Committee 委员会投票页面（P2）
**Objective:** 支持委员会投票场景的初始化和投票操作。

**Files:**
- Create: `frontend/src/pages/CommitteeVote/index.tsx`
- Create: `frontend/src/api/committee.ts`

---

### Task 9: 新增 Escalation 超时升级页面（P2）
**Objective:** 查看和处理超时未处理的流程任务。

**Files:**
- Create: `frontend/src/pages/Escalation/index.tsx`
- Create: `frontend/src/api/escalation.ts`

---

### Task 10: 新增 UserLifecycle 用户交接页面（P2）
**Objective:** 处理员工调岗、辞职时的流程交接。

**Files:**
- Create: `frontend/src/pages/UserLifecycle/index.tsx`
- Create: `frontend/src/api/userLifecycle.ts`

---

## 验证步骤

每个任务完成后：
1. `cd /root/flowable-engine-service/frontend && npm run build` — 确保编译通过
2. 重启 Vite 服务
3. 浏览器访问 http://localhost:3000 验证新功能
4. 外网 http://36.133.114.164:3000 验证

## 风险与注意事项

1. **后端 API 路径必须完全匹配** — 前端请求路径要和后端 @RequestMapping 一致
2. **JWT Token 传递** — 所有需要认证的 API 通过 Axios interceptor 自动携带 token
3. **权限控制** — 部分管理操作可能需要管理员权限，前端做路由守卫
4. **Vite proxy** — 开发环境 `/api` 代理到 `http://36.133.114.164:9999`
5. **不要修改后端代码** — 只在前端补全，后端 API 已经是完整的
