package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.service.FlowableQueryHelper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoricController {

    private final HistoryService historyService;
    private final FlowableQueryHelper queryHelper;

    /**
     * 历史流程实例列表
     */
    @GetMapping("/processes")
    public Result<Map<String, Object>> listProcesses(
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(value = "processDefinitionKey", required = false) String procKey,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        var query = historyService.createHistoricProcessInstanceQuery();

        // 核心：注入租户权限
        queryHelper.applyTenantFilter(query);

        if (StringUtils.hasText(businessKey)) {
            query.processInstanceBusinessKey(businessKey);
        }
        if (StringUtils.hasText(procKey)) {
            query.processDefinitionKey(procKey);
        }
        if (StringUtils.hasText(tenantId)) {
            query.processInstanceTenantId(tenantId);
        }

        long total = query.count();
        List<HistoricProcessInstance> instances = query.listPage((page - 1) * size, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("data", instances.stream().map(h -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", h.getId());
            vo.put("businessKey", h.getBusinessKey());
            vo.put("processDefinitionKey", h.getProcessDefinitionKey());
            vo.put("processDefinitionName", h.getProcessDefinitionName());
            vo.put("startTime", h.getStartTime());
            vo.put("endTime", h.getEndTime());
            vo.put("duration", h.getDurationInMillis());
            vo.put("tenantId", h.getTenantId());
            vo.put("startUserId", h.getStartUserId());
            return vo;
        }).collect(Collectors.toList()));
        return Result.success(result);
    }

    /**
     * 历史任务列表
     */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> listTasks(
            @RequestParam(value = "processInstanceId") String processInstanceId) {

        var query = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId);

        // 核心：注入租户权限
        queryHelper.applyTenantFilter(query);

        List<HistoricTaskInstance> tasks = query.list();
        return Result.success(tasks.stream().map(h -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", h.getId());
            vo.put("name", h.getName());
            vo.put("assignee", h.getAssignee());
            vo.put("createTime", h.getCreateTime());
            vo.put("endTime", h.getEndTime());
            vo.put("duration", h.getDurationInMillis());
            return vo;
        }).collect(Collectors.toList()));
    }
}
