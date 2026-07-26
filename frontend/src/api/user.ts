import request from '@/utils/request';

export interface User {
  id: string;
  username: string;
  realName: string;
  orgId: string;
  status?: number;
}

// 获取用户列表
export const getUserList = () =>
  request.get<any, { data: User[] }>('/auth/users');

// 创建用户
export const createUser = (data: Partial<User>) =>
  request.post<any, { data: User }>('/auth/users', data);

// 更新用户
export const updateUser = (id: string, data: Partial<User>) =>
  request.put<any, { data: User }>(`/auth/users/${id}`, data);

// 删除用户
export const deleteUser = (id: string) =>
  request.delete<any, { data: null }>(`/auth/users/${id}`);
