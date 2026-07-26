import request from '@/utils/request';

export interface Role {
  id: string;
  roleName: string;
  roleCode: string;
  scopeType: string;
  description?: string;
  status?: number;
}

// 获取角色列表
export const getRoleList = () =>
  request.get<any, { data: Role[] }>('/auth/roles');

// 创建角色
export const createRole = (data: Partial<Role>) =>
  request.post<any, { data: Role }>('/auth/roles', data);

// 更新角色
export const updateRole = (id: string, data: Partial<Role>) =>
  request.put<any, { data: Role }>(`/auth/roles/${id}`, data);

// 删除角色
export const deleteRole = (id: string) =>
  request.delete<any, { data: null }>(`/auth/roles/${id}`);
