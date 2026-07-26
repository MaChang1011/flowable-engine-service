import request from '@/utils/request';

export interface ProcessDefinition {
  id: string;
  processKey: string;
  processName: string;
  version: number;
  formSchemaId?: string;
  status: number;
}

// 获取流程定义列表
export const listProcessDefs = (params?: Record<string, any>) =>
  request.get<any, { data: ProcessDefinition[] }>('/process/definition/list', { params });

// 部署流程（multipart）
export const deployProcess = (file: File, tenantId?: string) => {
  const formData = new FormData();
  formData.append('file', file);
  if (tenantId) {
    formData.append('tenantId', tenantId);
  }
  return request.post<any, { data: any }>('/process/definition/deploy', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// 挂起流程定义
export const suspendDefinition = (id: string) =>
  request.post<any, { data: null }>(`/process/definition/${id}/suspend`);

// 激活流程定义
export const activateDefinition = (id: string) =>
  request.post<any, { data: null }>(`/process/definition/${id}/activate`);

// 删除部署
export const deleteDeployment = (id: string) =>
  request.delete<any, { data: null }>(`/process/definition/${id}`);
