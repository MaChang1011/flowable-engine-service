import request from '@/utils/request';

export interface EscalationTask {
  taskId: string;
  taskName: string;
  assignee?: string;
  processInstanceId: string;
  currentActivityName: string;
  createTime: string;
  overdueDuration?: string;
}

// 升级任务
export const escalateTask = (taskId: string) =>
  request.post<any, { data: null }>(`/wf/escalation/${taskId}`);

// 获取超时任务列表
export const getOverdueTasks = () =>
  request.get<any, { data: EscalationTask[] }>('/wf/escalation/overdue');
