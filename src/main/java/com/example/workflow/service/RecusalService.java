package com.example.workflow.service;

import com.example.workflow.dto.CompleteTaskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 回避规则服务 — 申请人不能审批自己的单子
 *
 * 检查点:
 * 1. 任务完成前：如果 assignee == applicant → 自动跳过/转给上级
 * 2. 任务分配时：如果 candidate == applicant → 从候选列表中移除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecusalService {

    private final TaskService taskService;

    /**
     * 检查并处理回避 —— 在 task/complete 前调用
     *
     * @return 如果触发了回避返回 true，正常审批返回 false
     */
    public boolean checkAndHandleRecusal(Task task, String currentUserId) {
        // 获取流程申请人
        Object applicantObj = taskService.getVariable(task.getId(), "applicant");
        if (applicantObj == null) return false;

        String applicant = applicantObj.toString();

        // 如果当前审批人 == 申请人，触发回避
        if (currentUserId.equals(applicant)) {
            log.warn("回避规则触发: taskId={}, 审批人={} 与申请人相同，自动跳过",
                    task.getId(), currentUserId);

            // 策略1: 自动完成（跳过该节点）
            // 策略2: 自动委派给上级（需要查 org tree）
            // 这里采用策略1：自动通过
            taskService.addComment(task.getId(), task.getProcessInstanceId(),
                    "系统自动处理：审批人与申请人相同，触发回避规则，自动通过");
            taskService.complete(task.getId());
            return true;
        }

        return false;
    }

    /**
     * 检查申请人的候选资格并移除
     */
    public boolean removeApplicantFromCandidates(Task task, String applicant) {
        try {
            taskService.deleteCandidateUser(task.getId(), applicant);
            log.info("回避: 从候选组移除申请人 taskId={}, applicant={}", task.getId(), applicant);
            return true;
        } catch (Exception e) {
            log.debug("回避: 删除候选失败 (可能不存在) taskId={}", task.getId());
            return false;
        }
    }
}
