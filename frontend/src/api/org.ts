import request from '@/utils/request';

export interface Org {
  id: string;
  orgName: string;
  parentId: string;
  orgLevel: number;
  sortOrder: number;
  status?: number;
  children?: Org[];
}

// 获取机构列表
export const getOrgList = () =>
  request.get<any, { data: Org[] }>('/auth/orgs');

// 创建机构
export const createOrg = (data: Partial<Org>) =>
  request.post<any, { data: Org }>('/auth/orgs', data);

// 更新机构
export const updateOrg = (id: string, data: Partial<Org>) =>
  request.put<any, { data: Org }>(`/auth/orgs/${id}`, data);

// 删除机构
export const deleteOrg = (id: string) =>
  request.delete<any, { data: null }>(`/auth/orgs/${id}`);
