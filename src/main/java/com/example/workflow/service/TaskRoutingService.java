package com.example.workflow.service;

import com.example.workflow.security.PermissionContext;
import com.example.workflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 跨机构任务路由服务
 * 
 * 处理跨机构审批时的任务分配逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRoutingService {

    private final TaskService taskService;
    private final OrgService orgService;

    /**
     * 为当前任务添加候选人（支持跨机构）
     * 
     * @param taskId 任务ID
     * @param userId 被指派人ID
     * @param orgId 被指派人的机构ID（可能跨机构）
     */
    public void assignTaskToUser(String taskId, String userId, String orgId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 检查权限：当前用户是否有权操作该机构的任务
        List<String> accessibleOrgIds = PermissionContext.getAccessibleOrgIds();
        if (accessibleOrgIds != null && !accessibleOrgIds.contains(orgId)) {
            log.warn("越权操作: 用户{}尝试操作机构{}的任务{}",
                    SecurityUtils.getCurrentUserId(), orgId, taskId);
            throw new SecurityException("无权操作该机构的任务");
        }

        // 设置任务负责人
        taskService.setAssignee(taskId, userId);
        log.info("任务指派成功: taskId={}, assignee={}, orgId={}", taskId, userId, orgId);
    }

    /**
     * 批量查询某机构下的所有待办任务
     */
    public List<Task> getTasksByOrgId(String orgId) {
        List<String> accessibleOrgIds = orgService.getAccessibleOrgIds(orgId);
        if (accessibleOrgIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 注意：Flowable 6.8 不支持直接按tenantId过滤Task，需要用processInstanceId关联
        // 这里通过查询所有活跃任务，然后过滤
        List<Task> allTasks = taskService.createTaskQuery().active().list();
        return allTasks.stream()
                .filter(task -> accessibleOrgIds.contains(task.getTenantId()))
                .collect(Collectors.toList());
    }

    /**
     * 转办任务
     */
    public void delegateTask(String taskId, String newAssignee, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        if (StringUtils.hasText(comment)) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        // 先保存原始assignee
        String originalAssignee = task.getAssignee();
        
        // 设置新的assignee
        taskService.setAssignee(taskId, newAssignee);
        
        log.info("任务转办: taskId={}, from={}, to={}", taskId, originalAssignee, newAssignee);
    }

    /**
     * 认领任务（从候选组中认领）
     */
    public void claimTask(String taskId, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        taskService.claim(taskId, userId);
        log.info("任务认领成功: taskId={}, userId={}", taskId, userId);
    }
}
