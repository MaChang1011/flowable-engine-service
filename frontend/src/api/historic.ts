import request from '@/utils/request';

export interface HistoricProcessInstance {
  id: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey?: string;
  startTime: string;
  endTime?: string;
  duration?: number;
  status: string;
  initiator?: string;
}

export interface HistoricTaskInstance {
  taskId: string;
  taskName: string;
  assignee?: string;
  processInstanceId: string;
  processDefinitionKey: string;
  startTime: string;
  endTime?: string;
  duration?: number;
  outcome?: string;
}

// 获取历史流程实例列表
export const listHistoricProcesses = (params?: Record<string, any>) =>
  request.get<any, { data: HistoricProcessInstance[] }>('/history/processes', { params });

// 获取历史任务列表
export const listHistoricTasks = (processInstanceId: string) =>
  request.get<any, { data: HistoricTaskInstance[] }>(`/history/tasks`, { params: { processInstanceId } });
