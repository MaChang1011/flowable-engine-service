package com.example.workflow.service;

import com.example.workflow.dto.CompleteTaskRequest;
import com.example.workflow.dto.StartProcessRequest;
import com.example.workflow.dto.vo.ProcessInstanceVO;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.entity.WfBusinessData;
import com.example.workflow.mapper.BusinessDataMapper;
import com.example.workflow.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowFacade {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final IdentityService identityService;
    private final FlowableQueryHelper queryHelper;
    private final BusinessDataMapper businessDataMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> startProcess(StartProcessRequest req) {
        String processDefinitionKey = req.getProcessDefinitionKey();
        String businessKey = req.getBusinessKey();
        String tenantId = StringUtils.hasText(req.getTenantId()) ? req.getTenantId() : SecurityUtils.getCurrentTenantId();
        String applicantId = StringUtils.hasText(req.getApplicantId()) ? req.getApplicantId() : SecurityUtils.getCurrentUserId();
        String applicantOrgId = StringUtils.hasText(req.getApplicantOrgId()) ? req.getApplicantOrgId() : SecurityUtils.getCurrentOrgId();

        if (StringUtils.hasText(applicantId)) {
            identityService.setAuthenticatedUserId(applicantId);
        }

        Map<String, Object> variables = req.getVariables() != null ? req.getVariables() : new HashMap<>();
        variables.put("applicant", applicantId);
        variables.put("applicantOrgId", applicantOrgId);

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processDefinitionKey)
                .businessKey(businessKey)
                .tenantId(tenantId)
                .variables(variables)
                .start();

        log.info("流程启动成功: processInstanceId={}, businessKey={}, tenantId={}",
                instance.getId(), businessKey, tenantId);

        WfBusinessData bd = new WfBusinessData();
        bd.setId(UUID.randomUUID().toString().replace("-", ""));
        bd.setProcessInstanceId(instance.getId());
        bd.setBusinessKey(businessKey);
        bd.setProcessKey(processDefinitionKey);
        bd.setTenantId(tenantId);
        bd.setApplicantId(applicantId);
        bd.setApplicantOrgId(applicantOrgId);
        try {
            bd.setBusinessData(objectMapper.writeValueAsString(variables));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
        }
        businessDataMapper.insert(bd);

        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("businessKey", businessKey);
        result.put("processDefinitionKey", processDefinitionKey);
        result.put("tenantId", tenantId);
        if (currentTask != null) {
            Map<String, Object> ct = new LinkedHashMap<>();
            ct.put("taskId", currentTask.getId());
            ct.put("taskName", currentTask.getName());
            ct.put("assignee", currentTask.getAssignee());
            ct.put("createTime", currentTask.getCreateTime());
            result.put("currentTask", ct);
        }
        return result;
    }

    @Transactional
    public void submitTask(CompleteTaskRequest req) {
        Task task = taskService.createTaskQuery().taskId(req.getTaskId()).singleResult();
        if (task == null) throw new IllegalArgumentException("任务不存在: " + req.getTaskId());

        String processInstanceId = task.getProcessInstanceId();
        if (StringUtils.hasText(req.getComment())) {
            taskService.addComment(req.getTaskId(), processInstanceId, req.getComment());
        }

        Map<String, Object> vars = req.getVariables() != null ? req.getVariables() : new HashMap<>();
        vars.put("approved", true);
        vars.put("comment", req.getComment());
        taskService.complete(req.getTaskId(), vars);
        log.info("任务完成: taskId={}, processInstanceId={}", req.getTaskId(), processInstanceId);
    }

    @Transactional
    public void rejectToNode(String taskId, String targetNodeId, Map<String, Object> variables) {
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask == null) throw new IllegalArgumentException("任务不存在: " + taskId);

        String processInstanceId = currentTask.getProcessInstanceId();
        String currentActivityId = currentTask.getTaskDefinitionKey();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentActivityId, targetNodeId)
                .changeState();

        if (variables != null && !variables.isEmpty()) {
            runtimeService.setVariables(processInstanceId, variables);
        }
        log.info("流程驳回到节点: taskId={}, targetNode={}", taskId, targetNodeId);
    }

    public List<TaskVO> getTodoTasks(String userId) {
        TaskQuery query = taskService.createTaskQuery();
        queryHelper.applyTenantFilter(query);
        query.taskCandidateOrAssigned(userId).active();
        query.orderByTaskCreateTime().desc();
        return query.list().stream().map(this::buildTaskVO).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDoneTasks(String userId) {
        var query = historyService.createHistoricTaskInstanceQuery()
                .finished().taskAssignee(userId);
        queryHelper.applyTenantFilter(query);
        return query.list().stream().map(task -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("taskId", task.getId());
            vo.put("taskName", task.getName());
            vo.put("assignee", task.getAssignee());
            vo.put("processInstanceId", task.getProcessInstanceId());
            vo.put("createTime", task.getCreateTime());
            vo.put("endTime", task.getEndTime());
            vo.put("duration", task.getDurationInMillis());
            return vo;
        }).collect(Collectors.toList());
    }

    public ProcessInstanceVO getProcessInstanceDetail(String processInstanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (instance == null) throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);

        ProcessInstanceVO vo = new ProcessInstanceVO();
        vo.setProcessInstanceId(instance.getId());
        vo.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setTenantId(instance.getTenantId());
        vo.setSuspended(instance.isSuspended());
        vo.setStartTime(instance.getStartTime() != null
                ? instance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : null);

        WfBusinessData bd = businessDataMapper.selectByProcessInstanceId(processInstanceId);
        if (bd != null) {
            vo.setApplicantId(bd.getApplicantId());
            vo.setApplicantOrgId(bd.getApplicantOrgId());
        }

        vo.setVariables(runtimeService.getVariables(processInstanceId));

        var activityQuery = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc();
        vo.setActivities(activityQuery.list().stream().map(a -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", a.getActivityId());
            info.put("activityName", a.getActivityName());
            info.put("activityType", a.getActivityType());
            info.put("startTime", a.getStartTime() != null
                    ? a.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : null);
            info.put("endTime", a.getEndTime() != null
                    ? a.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : null);
            info.put("assignee", a.getAssignee());
            return info;
        }).collect(Collectors.toList()));
        return vo;
    }

    public List<Map<String, Object>> getProcessTrace(String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc();
        return query.list().stream().map(a -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", a.getActivityId());
            info.put("activityName", a.getActivityName());
            info.put("activityType", a.getActivityType());
            info.put("startTime", a.getStartTime() != null
                    ? a.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : null);
            info.put("endTime", a.getEndTime() != null
                    ? a.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : null);
            info.put("duration", a.getDurationInMillis());
            info.put("assignee", a.getAssignee());
            return info;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getActiveActivities(String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).unfinished();
        return query.list().stream().map(a -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", a.getActivityId());
            info.put("activityName", a.getActivityName());
            info.put("activityType", a.getActivityType());
            return info;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        log.info("流程终止: processInstanceId={}, reason={}", processInstanceId, reason);
    }

    public void suspendProcess(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    public void activateProcess(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    private TaskVO buildTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getName());
        vo.setAssignee(task.getAssignee());
        vo.setProcessInstanceId(task.getProcessInstanceId());
        vo.setProcessDefinitionId(task.getProcessDefinitionId());
        vo.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
        vo.setDueDate(task.getDueDate() != null ? task.getDueDate().toString() : null);
        vo.setFormKey(task.getFormKey());
        vo.setTenantId(task.getTenantId());

        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
        if (pi != null) {
            List<HistoricActivityInstance> unfinished = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .unfinished().list();
            if (!unfinished.isEmpty()) {
                vo.setCurrentActivityName(unfinished.get(0).getActivityName());
            }
        }
        return vo;
    }
}
