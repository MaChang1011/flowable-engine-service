package com.example.workflow.service;

import com.example.workflow.entity.WfCommitteeVote;
import com.example.workflow.mapper.CommitteeVoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 审批委员会投票服务
 *
 * 流程:
 * 1. BPMN 中委员会节点使用多实例 userTask，为每个委员创建子任务
 * 2. 委员通过 API 提交投票 (APPROVE/REJECT/ABSTAIN)
 * 3. 达到投票阈值时自动完成父任务，流程继续
 *
 * 投票阈值由流程变量 committeeThreshold 指定（默认 >50%）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitteeVoteService {

    private final CommitteeVoteMapper voteMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    /**
     * 初始化委员会投票 — 在 BPMN 多实例任务创建时调用
     *
     * @param taskId 父任务 ID
     * @param committeeName 委员会名称
     * @param memberIds 委员 ID 列表
     */
    @Transactional
    public void initCommittee(String taskId, String committeeName, List<String> memberIds) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("初始化委员会失败: taskId={} 不存在", taskId);
            return;
        }

        for (String memberId : memberIds) {
            WfCommitteeVote vote = new WfCommitteeVote();
            vote.setId(UUID.randomUUID().toString().replace("-", ""));
            vote.setProcessInstanceId(task.getProcessInstanceId());
            vote.setTaskId(taskId);
            vote.setCommitteeName(committeeName);
            vote.setCommitteeMemberId(memberId);
            voteMapper.insert(vote);
        }

        log.info("委员会初始化: taskId={}, committee={}, members={}",
                taskId, committeeName, memberIds.size());
    }

    /**
     * 委员投票
     *
     * @param taskId 父任务 ID
     * @param memberId 委员 ID
     * @param vote APPROVE / REJECT / ABSTAIN
     * @param comment 投票意见
     * @param threshold 通过阈值 (0.0~1.0，如 0.6 表示 60% 通过)
     * @return 投票结果统计
     */
    @Transactional
    public Map<String, Object> castVote(String taskId, String memberId, String vote,
                                         String comment, double threshold) {
        // 更新该委员的投票
        List<WfCommitteeVote> records = voteMapper.selectByTaskId(taskId);
        WfCommitteeVote target = records.stream()
                .filter(v -> memberId.equals(v.getCommitteeMemberId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("委员不在该委员会中: " + memberId));

        if (target.getVote() != null) {
            throw new IllegalArgumentException("委员已投票: " + memberId);
        }

        voteMapper.updateVote(target.getId(), vote, comment);

        // 重新统计
        return tallyVotes(taskId, threshold);
    }

    /**
     * 统计投票结果
     *
     * @return {totalMembers, approved, rejected, abstained, passed, threshold}
     */
    public Map<String, Object> tallyVotes(String taskId, double threshold) {
        List<WfCommitteeVote> records = voteMapper.selectByTaskId(taskId);
        int total = records.size();
        long approved = records.stream().filter(v -> "APPROVE".equals(v.getVote())).count();
        long rejected = records.stream().filter(v -> "REJECT".equals(v.getVote())).count();
        long abstained = records.stream().filter(v -> "ABSTAIN".equals(v.getVote())).count();
        long voted = approved + rejected + abstained;

        // 通过条件：同意票占总人数比例 >= 阈值
        boolean passed = ((double) approved / total) >= threshold;
        // 无法通过：剩余未投票全投同意也不够
        double maxPossible = ((double) (approved + (total - voted)) / total);
        boolean impossible = maxPossible < threshold;

        // 自动完成：通过 / 无法通过 / 全部投完
        boolean shouldFinish = passed || impossible || voted == total;
        if (shouldFinish) {
            completeCommitteeTask(taskId, passed);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("totalMembers", total);
        result.put("approved", approved);
        result.put("rejected", rejected);
        result.put("abstained", abstained);
        result.put("voted", voted);
        result.put("threshold", threshold);
        result.put("passed", passed);
        result.put("finished", shouldFinish);
        return result;
    }

    /**
     * 完成委员会父任务
     */
    private void completeCommitteeTask(String taskId, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) return;

        // 取消未完成的子任务
        List<Execution> childExecs = runtimeService.createExecutionQuery()
                .parentId(task.getExecutionId()).list();
        for (Execution exec : childExecs) {
            runtimeService.deleteProcessInstance(exec.getId(), "委员会投票结束");
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("committeeApproved", approved);
        taskService.complete(taskId, vars);
        log.info("委员会审批完成: taskId={}, approved={}", taskId, approved);
    }

    /**
     * 查询投票详情
     */
    public List<Map<String, Object>> getVoteDetails(String taskId) {
        List<WfCommitteeVote> records = voteMapper.selectByTaskId(taskId);
        List<Map<String, Object>> details = new ArrayList<>();
        for (WfCommitteeVote v : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("memberId", v.getCommitteeMemberId());
            item.put("vote", v.getVote());
            item.put("comment", v.getComment());
            item.put("votedAt", v.getVotedAt());
            details.add(item);
        }
        return details;
    }
}
