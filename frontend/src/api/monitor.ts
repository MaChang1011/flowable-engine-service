import request from '@/utils/request';

// 监控概览
export const getMonitorDashboard = () => {
  return request.get<any, any>('/monitor/dashboard');
};

// 流程实例列表
export const listInstances = (params: { status?: string; processDefinitionKey?: string; businessKey?: string; page?: number; pageSize?: number }) => {
  return request.get<any, any>('/monitor/instances', { params });
};

// 流程轨迹
export const getTrace = (processInstanceId: string) => {
  return request.get<any, any>(`/monitor/trace/${processInstanceId}`);
};

// 当前活跃节点
export const getActiveNodes = (processInstanceId: string) => {
  return request.get<any, any>(`/monitor/active-nodes/${processInstanceId}`);
};
