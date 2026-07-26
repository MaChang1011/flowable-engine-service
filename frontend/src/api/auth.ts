import request from '@/utils/request';

export interface LoginParams {
  username: string;
  password: string;
}

export interface UserInfo {
  token: string;
  userId: string;
  username: string;
  realName: string;
  orgId: string;
  orgName: string;
  roleIds: string;
  scopeType: string;
  accessibleOrgIds: string[];
}

// 登录
export const login = (data: LoginParams) =>
  request.post<any, { data: UserInfo }>('/auth/login', data);

// 获取用户信息
export const getUserInfo = () =>
  request.get<any, { data: UserInfo }>('/auth/user/info');
