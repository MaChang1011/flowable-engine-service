import request from '@/utils/request';

export interface FormSchema {
  id: string;
  schemaKey?: string;
  name: string;
  schema: any;
  status: number;
  createTime: string;
  updateTime: string;
}

// 创建表单schema
export const createForm = (data: FormSchema) =>
  request.post<any, { data: FormSchema }>('/wf/form/schema', data);

// 更新表单schema
export const updateForm = (id: string, data: FormSchema) =>
  request.put<any, { data: FormSchema }>(`/wf/form/schema/${id}`, data);

// 删除表单schema
export const deleteForm = (id: string) =>
  request.delete<any, { data: null }>(`/wf/form/schema/${id}`);

// 获取表单schema列表
export const listForms = (status?: number, schemaKey?: string) =>
  request.get<any, { data: FormSchema[] }>('/wf/form/schema/list', { params: { status, schemaKey } });

// 根据ID获取表单schema
export const getFormById = (id: string) =>
  request.get<any, { data: FormSchema }>(`/wf/form/schema/${id}`);

// 根据key获取表单schema
export const getFormByKey = (key: string) =>
  request.get<any, { data: FormSchema }>(`/wf/form/schema/by-key/${key}`);

// 校验表单数据
export const validateFormData = (jsonSchema: any, businessData: any) =>
  request.post<any, { data: any }>('/wf/form/schema/validate', { jsonSchema, businessData });

// 获取表单字段
export const getFormFields = (id: string) =>
  request.get<any, { data: any }>(`/wf/form/schema/${id}/fields`);
