package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流统一服务层 —— 业务系统的唯一入口
 * 
 * 所有流程操作都通过此服务完成，对外暴露统一的 REST API。
 * 内部封装 Flowable 原生 API，屏蔽底层复杂度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final FlowableQueryHelper queryHelper;

    /**
     * 启动流程实例
     * 
     * @param processDefinitionKey 流程定义Key
     * @param businessKey 业务主键（关联你的业务单号）
     * @param tenantId 租户ID
     * @param variables 流程变量（表单数据、审批结果等）
     * @return 流程实例ID
     */
    public String startProcess(String processDefinitionKey, String businessKey, 
                                String tenantId, Map<String, Object> variables) {
        var builder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processDefinitionKey)
                .businessKey(businessKey);
        
        if (StringUtils.hasText(tenantId)) {
            builder.tenantId(tenantId);
        }
        if (variables != null && !variables.isEmpty()) {
            builder.variables(variables);
        }
        
        ProcessInstance instance = builder.start();
        log.info("流程启动成功: processInstanceId={}, businessKey={}", 
                instance.getId(), businessKey);
        return instance.getId();
    }

    /**
     * 提交任务（审批通过/驳回）
     * 
     * @param taskId 任务ID
     * @param variables 提交的数据（如 approved=true/false, comment=审批意见）
     * @param comment 审批备注
     */
    @Transactional
    public void submitTask(String taskId, Map<String, Object> variables, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        String processInstanceId = task.getProcessInstanceId();
        
        // 1. 先添加审批意见（必须在完成任务之前）
        if (StringUtils.hasText(comment)) {
            taskService.addComment(taskId, processInstanceId, comment);
        }
        
        // 2. 完成任务
        Map<String, Object> vars = variables != null ? variables : new HashMap<>();
        taskService.complete(taskId, vars);
        
        log.info("任务完成: taskId={}, processInstanceId={}", taskId, processInstanceId);
    }

    /**
     * 驳回到指定节点（回退）
     * 
     * 支持两种模式：
     * 1. rejectToStart() → 驳回到流程开始
     * 2. rejectToNode(processInstanceId, targetNodeId) → 驳回到指定节点
     * 
     * @param taskId 当前任务ID
     * @param targetNodeId 目标节点ID（如 "apply"、"manager_approve"）
     * @param variables 驳回时携带的变量
     */
    @Transactional
    public void rejectToNode(String taskId, String targetNodeId, 
                              Map<String, Object> variables) {
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        String processInstanceId = currentTask.getProcessInstanceId();
        String currentActivityId = currentTask.getTaskDefinitionKey();
        
        // 1. 将流程实例移动到目标节点
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetNodeId)
                .changeState();
        
        // 2. 设置变量
        if (variables != null && !variables.isEmpty()) {
            runtimeService.setVariables(processInstanceId, variables);
        }
        
        log.info("流程驳回到节点: taskId={}, targetNode={}, processInstanceId={}", 
                taskId, targetNodeId, processInstanceId);
    }

    /**
     * 驳回到流程开始
     */
    @Transactional
    public void rejectToStart(String taskId, Map<String, Object> variables) {
        rejectToNode(taskId, "start", variables);
    }

    /**
     * 查询用户的待办任务列表（带租户过滤）
     */
    public List<Map<String, Object>> getTodoTasks(String userId) {
        TaskQuery query = taskService.createTaskQuery();
        queryHelper.applyTenantFilter(query);
        query.taskCandidateOrAssigned(userId).active();
        
        return query.list().stream().map(this::buildTaskInfo).collect(Collectors.toList());
    }

    /**
     * 查询用户的已办任务列表
     */
    public List<Map<String, Object>> getDoneTasks(String userId) {
        var query = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskAssignee(userId);
        queryHelper.applyTenantFilter(query);
        
        return query.list().stream().map(this::buildDoneTaskInfo).collect(Collectors.toList());
    }

    /**
     * 查询流程实例详情
     */
    public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", instance.getId());
        result.put("processDefinitionKey", instance.getProcessDefinitionKey());
        result.put("businessKey", instance.getBusinessKey());
        result.put("tenantId", instance.getTenantId());
        result.put("suspended", instance.isSuspended());
        result.put("startTime", instance.getStartTime() != null 
                ? instance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() 
                : null);
        
        // 附带所有流程变量
        result.put("variables", runtimeService.getVariables(processInstanceId));
        
        return result;
    }

    /**
     * 获取流程轨迹（已执行的活动列表）
     */
    public List<Map<String, Object>> getProcessTrace(String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc();
        
        return query.list().stream().map(activity -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", activity.getActivityId());
            info.put("activityName", activity.getActivityName());
            info.put("activityType", activity.getActivityType());
            info.put("startTime", activity.getStartTime() != null
                    ? activity.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : null);
            info.put("endTime", activity.getEndTime() != null
                    ? activity.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : null);
            info.put("duration", activity.getDurationInMillis());
            info.put("assignee", activity.getAssignee());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 获取当前流程的活跃活动（未完成的节点）
     */
    public List<Map<String, Object>> getActiveActivities(String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .unfinished();
        
        return query.list().stream().map(activity -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", activity.getActivityId());
            info.put("activityName", activity.getActivityName());
            info.put("activityType", activity.getActivityType());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 终止流程实例
     */
    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        log.info("流程终止: processInstanceId={}, reason={}", processInstanceId, reason);
    }

    /**
     * 挂起/激活流程实例
     */
    public void suspendProcess(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    public void activateProcess(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    /**
     * 删除流程实例后续的所有活动（用于驳回时清理）
     */
    private void deleteProcessInstanceActivities(String processInstanceId) {
        List<HistoricActivityInstance> unfinished = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .unfinished()
                .list();
        
        for (HistoricActivityInstance activity : unfinished) {
            if ("userTask".equals(activity.getActivityType())) {
                Task task = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (task != null) {
                    taskService.deleteTask(task.getId(), "驳回到上游节点");
                }
            }
        }
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> buildTaskInfo(Task task) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("taskId", task.getId());
        vo.put("taskName", task.getName());
        vo.put("assignee", task.getAssignee());
        vo.put("processInstanceId", task.getProcessInstanceId());
        vo.put("processDefinitionId", task.getProcessDefinitionId());
        vo.put("createTime", task.getCreateTime());
        vo.put("dueDate", task.getDueDate());
        vo.put("formKey", task.getFormKey());
        // 获取候选人/候选组（通过 identity links）
        var identityLinks = taskService.getIdentityLinksForTask(task.getId());
        vo.put("identityLinks", identityLinks.stream().map(il -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("type", il.getType());
            m.put("userId", il.getUserId());
            m.put("groupId", il.getGroupId());
            return m;
        }).collect(Collectors.toList()));
        return vo;
    }

    private Map<String, Object> buildDoneTaskInfo(HistoricTaskInstance task) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("taskId", task.getId());
        vo.put("taskName", task.getName());
        vo.put("assignee", task.getAssignee());
        vo.put("processInstanceId", task.getProcessInstanceId());
        vo.put("createTime", task.getCreateTime());
        vo.put("endTime", task.getEndTime());
        vo.put("duration", task.getDurationInMillis());
        return vo;
    }
}
