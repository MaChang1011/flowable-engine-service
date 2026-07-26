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
