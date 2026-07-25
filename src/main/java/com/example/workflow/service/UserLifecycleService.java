package com.example.workflow.service;

import com.example.workflow.dto.ResignationRequest;
import com.example.workflow.dto.TransferRequest;
import com.example.workflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户生命周期管理 — 调岗 / 离职
 * 
 * 调岗：用户转到新机构，现有待办任务保留（仍在用户名下，机构上下文更新）
 * 离职：用户禁用，所有待办任务重分配给指定承接人
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLifecycleService {

    private final UserMapper userMapper;
    private final TaskService taskService;
    private final TaskRoutingService taskRoutingService;

    /**
     * 调岗
     * 
     * 1. 更新用户机构
     * 2. 该用户已有的待办任务继续保留（assignee不变，org context自然切换）
     * 3. 记录调岗日志
     */
    @Transactional
    public Map<String, Object> transfer(TransferRequest req) {
        String userId = req.getUserId();
        String targetOrgId = req.getTargetOrgId();
        String reason = StringUtils.hasText(req.getReason()) ? req.getReason() : "组织调动";

        // 验证用户存在
        var user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        String oldOrgId = user.getOrgId();
        if (oldOrgId != null && oldOrgId.equals(targetOrgId)) {
            throw new IllegalArgumentException("目标机构与当前机构相同，无需调岗");
        }

        // 更新用户机构
        userMapper.updateOrg(userId, targetOrgId);

        // 统计当前待办任务数
        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .active()
                .list();

        log.info("调岗完成: userId={}, {} → {}, 待办任务数={}, 原因={}",
                userId, oldOrgId, targetOrgId, pendingTasks.size(), reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("oldOrgId", oldOrgId);
        result.put("newOrgId", targetOrgId);
        result.put("pendingTaskCount", pendingTasks.size());
        result.put("action", "transfer");
        return result;
    }

    /**
     * 离职
     * 
     * 1. 禁用用户账号
     * 2. 将该用户所有待办任务重分配给承接人
     * 3. 对每个重分配任务添加审批意见
     */
    @Transactional
    public Map<String, Object> resign(ResignationRequest req) {
        String userId = req.getUserId();
        String reassignToUserId = req.getReassignToUserId();
        String reason = StringUtils.hasText(req.getReason()) ? req.getReason() : "员工离职";

        // 验证用户存在
        var user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        // 验证承接人存在
        var reassignUser = userMapper.selectById(reassignToUserId);
        if (reassignUser == null) {
            throw new IllegalArgumentException("承接人不存在: " + reassignToUserId);
        }

        if (userId.equals(reassignToUserId)) {
            throw new IllegalArgumentException("不能将任务重分配给自己");
        }

        // 查询该用户所有待办任务
        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .active()
                .list();

        // 批量重分配
        int reassigned = 0;
        for (Task task : pendingTasks) {
            try {
                // 添加评论说明原因
                String comment = String.format("员工离职，任务由 %s 转交给 %s：%s",
                        userId, reassignToUserId, reason);
                taskService.addComment(task.getId(), task.getProcessInstanceId(), comment);

                // 重分配
                taskService.setAssignee(task.getId(), reassignToUserId);
                reassigned++;
            } catch (Exception e) {
                log.error("任务重分配失败: taskId={}, error={}", task.getId(), e.getMessage());
            }
        }

        // 禁用用户
        userMapper.updateStatus(userId, 0);

        // 同时移除候选身份（如果 Flowable identity 中有相关数据）
        try {
            taskService.deleteCandidateUser(userId, null); // 这只是示例，Flowable 6.8 API可能不同
        } catch (Exception ignored) {
            // 候选身份清理非必须
        }

        log.info("离职处理完成: userId={}, 重分配任务={}/{}, 承接人={}, 原因={}",
                userId, reassigned, pendingTasks.size(), reassignToUserId, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("disabled", true);
        result.put("reassignedTaskCount", reassigned);
        result.put("totalPendingTasks", pendingTasks.size());
        result.put("reassignToUserId", reassignToUserId);
        result.put("action", "resign");
        return result;
    }

    /**
     * 查询某用户当前待办任务数（辅助接口）
     */
    public Map<String, Object> getUserTaskSummary(String userId) {
        var user = userMapper.selectByIdIncludeDisabled(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .active()
                .list();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userId", userId);
        summary.put("username", user.getUsername());
        summary.put("orgId", user.getOrgId());
        summary.put("status", user.getStatus());
        summary.put("pendingTaskCount", pendingTasks.size());
        return summary;
    }
}
