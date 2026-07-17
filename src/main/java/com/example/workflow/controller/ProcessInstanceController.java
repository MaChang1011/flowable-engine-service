package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.dto.vo.ProcessInstanceVO;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.FlowableQueryHelper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process/instance")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final RuntimeService runtimeService;
    private final FlowableQueryHelper queryHelper;

    /**
     * 启动流程实例
     */
    @PostMapping("/start")
    public Result<ProcessInstanceVO> start(@RequestBody StartProcessRequest req) {
        Map<String, Object> variables = Optional.ofNullable(req.getVariables()).orElse(Map.of());
        
        String tenantId = StringUtils.hasText(req.getTenantId())
                ? req.getTenantId()
                : SecurityUtils.getCurrentTenantId();

        var builder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(req.getProcessDefinitionKey())
                .businessKey(req.getBusinessKey())
                .variables(variables);

        if (StringUtils.hasText(tenantId)) {
            builder.tenantId(tenantId);
        }

        ProcessInstance instance = builder.start();
        return Result.success(toVO(instance));
    }

    /**
     * 查询我的实例（带租户权限过滤）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        var query = runtimeService.createProcessInstanceQuery();

        // 核心：注入租户权限
        queryHelper.applyTenantFilter(query);

        if (StringUtils.hasText(businessKey)) {
            query.processInstanceBusinessKey(businessKey);
        }

        long total = query.count();
        List<ProcessInstance> instances = query.listPage((page - 1) * size, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("data", instances.stream().map(this::toVO).collect(Collectors.toList()));
        return Result.success(result);
    }

    /**
     * 查询实例详情
     */
    @GetMapping("/{id}")
    public Result<ProcessInstanceVO> getById(@PathVariable String id) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(id).singleResult();
        if (instance == null) {
            return Result.fail("流程实例不存在: " + id);
        }
        return Result.success(toVO(instance));
    }

    /**
     * 撤销/终止流程实例
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable String id) {
        runtimeService.deleteProcessInstance(id, "用户主动撤销");
        return Result.success();
    }

    /**
     * 挂起流程实例
     */
    @PostMapping("/{id}/suspend")
    public Result<Void> suspend(@PathVariable String id) {
        runtimeService.suspendProcessInstanceById(id);
        return Result.success();
    }

    /**
     * 激活流程实例
     */
    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable String id) {
        runtimeService.activateProcessInstanceById(id);
        return Result.success();
    }

    private ProcessInstanceVO toVO(ProcessInstance inst) {
        return new ProcessInstanceVO(
            inst.getId(),
            inst.getProcessDefinitionId(),
            inst.getProcessDefinitionKey(),
            inst.getBusinessKey(),
            inst.getTenantId(),
            inst.isSuspended(),
            !inst.isSuspended(),
            inst.getStartTime() != null ? inst.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}
