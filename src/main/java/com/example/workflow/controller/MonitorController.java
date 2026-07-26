package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.service.FlowableQueryHelper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程监控 Dashboard API
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final FlowableQueryHelper queryHelper;

    /**
     * 监控概览统计
     * GET /api/monitor/dashboard
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 进行中流程统计（按流程定义分组）
        List<ProcessInstance> runningInstances = runtimeService.createProcessInstanceQuery()
                .active().list();
        
        Map<String, Long> runningByType = runningInstances.stream()
                .collect(Collectors.groupingBy(
                        ProcessInstance::getProcessDefinitionKey,
                        Collectors.counting()
                ));
        
        Map<String, Object> runningSummary = new LinkedHashMap<>();
        runningSummary.put("total", runningInstances.size());
        runningSummary.put("byType", runningByType);
        
        // 2. 已完成流程统计
        long completedCount = historyService.createHistoricProcessInstanceQuery()
                .finished().count();
        
        Map<String, Long> completedByType = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        HistoricProcessInstance::getProcessDefinitionKey,
                        Collectors.counting()
                ));
        
        Map<String, Object> completedSummary = new LinkedHashMap<>();
        completedSummary.put("total", completedCount);
        completedSummary.put("byType", completedByType);

        // 3. 待办任务统计
        long todoCount = taskService.createTaskQuery().active().count();

        // 4. 各流程定义总览（含进行中+已完成）
        List<Map<String, Object>> processDefs = new ArrayList<>();
        
        // 从运行中的实例获取定义信息
        Set<String> defKeys = new HashSet<>();
        for (ProcessInstance pi : runningInstances) {
            defKeys.add(pi.getProcessDefinitionKey());
        }
        for (HistoricProcessInstance hpi : historyService.createHistoricProcessInstanceQuery().list()) {
            defKeys.add(hpi.getProcessDefinitionKey());
        }
        
        for (String key : defKeys) {
            long running = runningByType.getOrDefault(key, 0L);
            long completed = completedByType.getOrDefault(key, 0L);
            
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("key", key);
            def.put("running", running);
            def.put("completed", completed);
            def.put("total", running + completed);
            processDefs.add(def);
        }

        data.put("running", runningSummary);
        data.put("completed", completedSummary);
        data.put("todoCount", todoCount);
        data.put("processDefinitions", processDefs);
        data.put("timestamp", System.currentTimeMillis());

        return Result.success(data);
    }

    /**
     * 流程实例列表（支持分页+筛选）
     * GET /api/monitor/instances?status=running|completed&page=1&size=20
     */
    @GetMapping("/instances")
    public Result<Map<String, Object>> listInstances(
            @RequestParam(value = "status", defaultValue = "running") String status,
            @RequestParam(value = "processDefinitionKey", required = false) String procKey,
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if ("running".equals(status)) {
            var query = runtimeService.createProcessInstanceQuery().active();
            if (procKey != null && !procKey.isEmpty()) query.processDefinitionKey(procKey);
            if (businessKey != null && !businessKey.isEmpty()) query.processInstanceBusinessKey(businessKey);
            
            long total = query.count();
            List<ProcessInstance> instances = query.listPage((page - 1) * size, size);
            
            List<Map<String, Object>> data = instances.stream().map(this::buildRunningInstance).collect(Collectors.toList());
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", total);
            result.put("data", data);
            return Result.success(result);
        } else {
            var query = historyService.createHistoricProcessInstanceQuery().finished();
            if (procKey != null && !procKey.isEmpty()) query.processDefinitionKey(procKey);
            if (businessKey != null && !businessKey.isEmpty()) query.processInstanceBusinessKey(businessKey);
            
            long total = query.count();
            List<HistoricProcessInstance> instances = query.listPage((page - 1) * size, size);
            
            List<Map<String, Object>> data = instances.stream().map(this::buildCompletedInstance).collect(Collectors.toList());
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", total);
            result.put("data", data);
            return Result.success(result);
        }
    }

    /**
     * 流程活动执行时间线
     * GET /api/monitor/trace/{processInstanceId}
     */
    @GetMapping("/trace/{processInstanceId}")
    public Result<List<Map<String, Object>>> getTrace(@PathVariable String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc();
        
        List<Map<String, Object>> trace = query.list().stream().map(activity -> {
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
            // 判断状态
            if (activity.getEndTime() == null) {
                info.put("status", "running");
            } else {
                info.put("status", "completed");
            }
            return info;
        }).collect(Collectors.toList());
        
        return Result.success(trace);
    }

    /**
     * 当前活跃节点
     * GET /api/monitor/active-nodes/{processInstanceId}
     */
    @GetMapping("/active-nodes/{processInstanceId}")
    public Result<List<Map<String, Object>>> getActiveNodes(@PathVariable String processInstanceId) {
        var query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .unfinished();
        
        List<Map<String, Object>> active = query.list().stream().map(activity -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("activityId", activity.getActivityId());
            info.put("activityName", activity.getActivityName());
            info.put("activityType", activity.getActivityType());
            return info;
        }).collect(Collectors.toList());
        
        return Result.success(active);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildRunningInstance(ProcessInstance pi) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", pi.getId());
        vo.put("businessKey", pi.getBusinessKey());
        vo.put("processDefinitionKey", pi.getProcessDefinitionKey());
        vo.put("processDefinitionName", pi.getProcessDefinitionName());
        vo.put("startTime", pi.getStartTime() != null 
                ? pi.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null);
        vo.put("status", "running");
        vo.put("suspended", pi.isSuspended());
        return vo;
    }

    private Map<String, Object> buildCompletedInstance(HistoricProcessInstance hpi) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", hpi.getId());
        vo.put("businessKey", hpi.getBusinessKey());
        vo.put("processDefinitionKey", hpi.getProcessDefinitionKey());
        vo.put("processDefinitionName", hpi.getProcessDefinitionName());
        vo.put("startTime", hpi.getStartTime() != null 
                ? hpi.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null);
        vo.put("endTime", hpi.getEndTime() != null 
                ? hpi.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null);
        vo.put("duration", hpi.getDurationInMillis());
        vo.put("status", "completed");
        return vo;
    }
}
