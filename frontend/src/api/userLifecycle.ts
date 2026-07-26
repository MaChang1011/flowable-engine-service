import request from '@/utils/request';

export interface TransferRequest {
  fromUserId: string;
  toUserId: string;
  reason?: string;
}

export interface ResignRequest {
  userId: string;
  handoverUserId?: string;
  reason?: string;
}

export interface UserSummary {
  userId: string;
  username: string;
  pendingCount: number;
  doneCount: number;
  initiatedCount: number;
}

// 用户转移
export const transferUser = (req: TransferRequest) =>
  request.post<any, { data: null }>('/wf/user/transfer', req);

// 用户离职
export const resignUser = (req: ResignRequest) =>
  request.post<any, { data: null }>('/wf/user/resign', req);

// 获取用户汇总信息
export const getUserSummary = (userId: string) =>
  request.get<any, { data: UserSummary }>(`/wf/user/${userId}/summary`);
