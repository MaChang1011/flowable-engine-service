import request from '@/utils/request';

export interface VoteResult {
  taskId: string;
  committeeName: string;
  memberIds: string[];
  votes: Array<{
    memberId: string;
    vote: string;
    comment?: string;
    voteTime: string;
  }>;
  result: string;
}

// 初始化委员会
export const initCommittee = (taskId: string, committeeName: string, memberIds: string[]) =>
  request.post<any, { data: any }>('/wf/committee/init', { taskId, committeeName, memberIds });

// 投票
export const castVote = (taskId: string, memberId: string, vote: string, comment?: string, threshold?: number) =>
  request.post<any, { data: any }>('/wf/committee/vote', { taskId, memberId, vote, comment, threshold });

// 获取计票结果
export const getTally = (taskId: string, threshold?: number) =>
  request.get<any, { data: VoteResult }>('/wf/committee/' + taskId + '/tally', { params: { threshold } });

// 获取投票详情
export const getVoteDetails = (taskId: string) =>
  request.get<any, { data: VoteResult }>(`/wf/committee/${taskId}/details`);
