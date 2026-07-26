import request from '@/utils/request';

export interface ApprovalTemplate {
  id: string;
  name: string;
  processKey?: string;
  content: any;
  status: number;
  createTime: string;
  updateTime: string;
}

// 获取审批模板列表
export const listTemplates = (processKey?: string) =>
  request.get<any, { data: ApprovalTemplate[] }>('/wf/template/list', { params: { processKey } });

// 获取审批模板详情
export const getTemplate = (id: string) =>
  request.get<any, { data: ApprovalTemplate }>(`/wf/template/${id}`);

// 创建审批模板
export const createTemplate = (data: ApprovalTemplate) =>
  request.post<any, { data: ApprovalTemplate }>('/wf/template', data);

// 更新审批模板
export const updateTemplate = (id: string, data: ApprovalTemplate) =>
  request.put<any, { data: ApprovalTemplate }>(`/wf/template/${id}`, data);

// 切换审批模板状态
export const toggleTemplate = (id: string, status: number) =>
  request.post<any, { data: null }>(`/wf/template/${id}/toggle`, { status });

// 删除审批模板
export const deleteTemplate = (id: string) =>
  request.delete<any, { data: null }>(`/wf/template/${id}`);
