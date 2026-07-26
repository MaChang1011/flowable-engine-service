import request from '@/utils/request';

export interface ProcessDefinition {
  id: string;
  processKey: string;
  processName: string;
  version: number;
  formSchemaId?: string;
  status: number;
}

export interface Task {
  taskId: string;
  taskName: string;
  assignee?: string;
  processInstanceId: string;
  processDefinitionKey?: string;
  currentActivityName: string;
  createTime: string;
}

// 获取流程定义列表
export const getProcessList = () =>
  request.get<any, { data: ProcessDefinition[] }>('/wf/process/define/list');

// 获取待办任务
export const getTodoTasks = () =>
  request.get<any, { data: Task[] }>('/wf/task/todo');

// 完成任务
export const completeTask = (taskId: string, variables: Record<string, any>) =>
  request.post<any, { data: null }>('/wf/task/complete', { taskId, variables });

// 启动流程
export const startProcess = (processDefinitionKey: string, businessKey: string, variables: Record<string, any>) =>
  request.post<any, { data: any }>('/workflow/start', { processDefinitionKey, businessKey, variables });

// 获取表单schema
export const getFormSchema = (formSchemaId: string) =>
  request.get<any, { data: any }>(`/wf/form/schema/${formSchemaId}`);

// 获取流程实例详情
export const getInstanceDetail = (processInstanceId: string) =>
  request.get<any, { data: any }>(`/workflow/instance/${processInstanceId}`);

// 获取流程轨迹
export const getProcessTrace = (processInstanceId: string) =>
  request.get<any, { data: any }>(`/workflow/trace/${processInstanceId}`);

// 获取当前活跃节点
export const getActiveNodes = (processInstanceId: string) =>
  request.get<any, { data: any }>(`/workflow/active-nodes/${processInstanceId}`);

// 终止流程
export const terminateProcess = (processInstanceId: string, reason?: string) =>
  request.post<any, { data: null }>(`/workflow/terminate/${processInstanceId}`, { reason });

// 挂起流程
export const suspendProcess = (processInstanceId: string) =>
  request.post<any, { data: null }>(`/workflow/suspend/${processInstanceId}`);

// 激活流程
export const activateProcess = (processInstanceId: string) =>
  request.post<any, { data: null }>(`/workflow/activate/${processInstanceId}`);

// 驳回到发起人
export const rejectToStart = (taskId: string, variables?: Record<string, any>) =>
  request.post<any, { data: null }>('/workflow/reject-to-start', { taskId, variables });

// 驳回到指定节点
export const rejectTask = (taskId: string, targetNodeId: string, variables?: Record<string, any>) =>
  request.post<any, { data: null }>('/workflow/reject', { taskId, targetNodeId, variables });
