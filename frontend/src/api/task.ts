import request from '@/utils/request';

export interface Task {
  taskId: string;
  taskName: string;
  assignee?: string;
  processInstanceId: string;
  processDefinitionKey?: string;
  currentActivityName: string;
  createTime: string;
}

// 获取待办任务
export const getTodoTasks = (userId?: string) =>
  request.get<any, { data: Task[] }>('/wf/task/todo', { params: { userId } });

// 获取已办任务
export const getDoneTasks = (userId?: string) =>
  request.get<any, { data: Task[] }>('/wf/task/done', { params: { userId } });

// 完成任务
export const completeTask = (taskId: string, variables?: Record<string, any>) =>
  request.post<any, { data: null }>('/wf/task/complete', { taskId, variables });

// 驳回到指定节点
export const rejectToNode = (taskId: string, targetNodeId: string, variables?: Record<string, any>) =>
  request.post<any, { data: null }>('/wf/task/reject-to-node', { taskId, targetNodeId, variables });

// 转派任务
export const delegateTask = (taskId: string, newAssignee: string, comment?: string) =>
  request.post<any, { data: null }>('/wf/task/delegate', { taskId, newAssignee, comment });

// 签收任务
export const claimTask = (taskId: string, userId: string) =>
  request.post<any, { data: null }>('/wf/task/claim', { taskId, userId });

// 分配任务
export const assignTask = (taskId: string, userId: string, orgId?: string) =>
  request.post<any, { data: null }>('/wf/task/assign', { taskId, userId, orgId });

// 获取任务的表单Schema（用于动态表单渲染）
export const getTaskFormSchema = (taskId: string) =>
  request.get<any, { data: {
    hasSchema: boolean;
    schemaId?: string;
    schemaName?: string;
    jsonSchema?: string;
    uiSchema?: string;
    fields: Array<{
      name: string;
      type: string;
      title: string;
      required: boolean;
      widget?: string;
      minimum?: number;
      maximum?: number;
      maxLength?: number;
      options?: string[];
    }>;
    message?: string;
  } }>(`/wf/task/${taskId}/form-schema`);
