package com.example.workflow.service;

import com.example.workflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 时效升级服务 — 超时未审批自动 escalation
 *
 * 检查逻辑（定时任务，每5分钟扫描一次）:
 * 1. 查所有活跃任务
 * 2. 计算已等待时间
 * 3. 超过 timeoutHours 的 → escalation
 *
 * Escalation 策略:
 * - 重新分配给上级（通过 sys_org 的 parent_id 找上级机构的管理者）
 * - 添加评论记录原因
 * - 标记节点已升级（防止重复升级）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationService {

    private final TaskService taskService;
    private final UserMapper userMapper;

    /** 默认超时小时数 */
    private static final int DEFAULT_TIMEOUT_HOURS = 48;

    /**
     * 定时扫描超时任务（每5分钟）
     */
    @Scheduled(fixedRate = 300_000)
    public void scanAndEscalate() {
        List<Task> overdueTasks = findOverdueTasks();
        for (Task task : overdueTasks) {
            try {
                escalateTask(task);
            } catch (Exception e) {
                log.error("任务升级失败: taskId={}, error={}", task.getId(), e.getMessage());
            }
        }
    }

    /**
     * 手动触发升级
     */
    public Map<String, Object> manualEscalate(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return escalateTask(task);
    }

    /**
     * 查所有超时未审批的活跃任务
     */
    private List<Task> findOverdueTasks() {
        List<Task> allTasks = taskService.createTaskQuery().active().list();
        List<Task> overdue = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        for (Task task : allTasks) {
            LocalDateTime created = LocalDateTime.ofInstant(
                    task.getCreateTime().toInstant(), ZoneId.systemDefault());
            long hours = Duration.between(created, now).toHours();

            // 从流程变量读取超时设置
            int timeoutHours = getTimeoutHours(task);
            if (hours >= timeoutHours) {
                // 检查是否已升级过
                Object escalated = taskService.getVariable(task.getId(), "_escalated");
                if (escalated == null) {
                    overdue.add(task);
                }
            }
        }
        return overdue;
    }

    private int getTimeoutHours(Task task) {
        try {
            Object timeoutObj = taskService.getVariable(task.getId(), "timeoutHours");
            if (timeoutObj instanceof Number) {
                return ((Number) timeoutObj).intValue();
            }
        } catch (Exception ignored) {}
        return DEFAULT_TIMEOUT_HOURS;
    }

    /**
     * 升级单个任务
     */
    private Map<String, Object> escalateTask(Task task) {
        String oldAssignee = task.getAssignee();
        long hours = Duration.between(
                LocalDateTime.ofInstant(task.getCreateTime().toInstant(), ZoneId.systemDefault()),
                LocalDateTime.now()).toHours();

        // 获取升级目标（从流程变量或 org 上级）
        String escalateTo = resolveEscalateTarget(task);

        // 添加升级评论
        String comment = String.format(
                "【自动升级】任务已等待 %d 小时，原审批人 %s，自动升级至 %s",
                hours, oldAssignee != null ? oldAssignee : "未分配", escalateTo);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), comment);

        // 重新分配
        if (escalateTo != null) {
            taskService.setAssignee(task.getId(), escalateTo);
        }

        // 标记已升级
        taskService.setVariable(task.getId(), "_escalated", true);
        taskService.setVariable(task.getId(), "_escalatedFrom", oldAssignee);

        log.warn("任务升级: taskId={}, 等待{}小时, {} → {}",
                task.getId(), hours, oldAssignee, escalateTo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId());
        result.put("taskName", task.getName());
        result.put("waitedHours", hours);
        result.put("oldAssignee", oldAssignee);
        result.put("newAssignee", escalateTo);
        result.put("action", "escalated");
        return result;
    }

    /**
     * 解析升级目标
     * 优先级: 流程变量 superior > 同级机构管理者 > org 上级管理者
     */
    private String resolveEscalateTarget(Task task) {
        // 1. 流程变量
        try {
            Object sup = taskService.getVariable(task.getId(), "superior");
            if (sup != null) return sup.toString();
        } catch (Exception ignored) {}

        // 2. 从流程变量读取 escalateTo（审批链中定义）
        try {
            Object et = taskService.getVariable(task.getId(), "escalateTo");
            if (et != null) return et.toString();
        } catch (Exception ignored) {}

        // 3. 查找当前 assignee 的同一机构同事
        String currentAssignee = task.getAssignee();
        if (currentAssignee != null) {
            var user = userMapper.selectById(currentAssignee);
            if (user != null) {
                // 找同机构其他用户
                var colleagues = userMapper.selectByOrgId(user.getOrgId());
                if (!colleagues.isEmpty()) {
                    return colleagues.get(0).getId();
                }
            }
        }

        return "admin"; // 兜底：管理员
    }

    /**
     * 查询当前超时任务列表
     */
    public List<Map<String, Object>> getOverdueTasks() {
        List<Task> overdue = findOverdueTasks();
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Task t : overdue) {
            LocalDateTime created = LocalDateTime.ofInstant(
                    t.getCreateTime().toInstant(), ZoneId.systemDefault());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", t.getId());
            item.put("taskName", t.getName());
            item.put("assignee", t.getAssignee());
            item.put("waitedHours", Duration.between(created, now).toHours());
            item.put("timeoutHours", getTimeoutHours(t));
            result.add(item);
        }
        return result;
    }
}
