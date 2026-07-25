package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.entity.WfApprovalTemplate;
import com.example.workflow.mapper.ApprovalTemplateMapper;
import com.example.workflow.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 审批模板控制器 — 管理条件驱动的动态审批链规则
 */
@Tag(name = "审批模板", description = "动态审批链规则管理")
@RestController
@RequestMapping("/api/wf/template")
@RequiredArgsConstructor
public class ApprovalTemplateController {

    private final ApprovalTemplateMapper templateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "创建审批模板")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        WfApprovalTemplate tpl = new WfApprovalTemplate();
        tpl.setId(UUID.randomUUID().toString().replace("-", ""));
        tpl.setProcessKey((String) body.get("processKey"));
        tpl.setTemplateName((String) body.get("templateName"));
        tpl.setRuleType((String) body.getOrDefault("ruleType", "AMOUNT").toString());
        // rule_config 必须序列化为合法 JSON 字符串
        Object ruleConfig = body.get("ruleConfig");
        try {
            tpl.setRuleConfig(objectMapper.writeValueAsString(ruleConfig));
        } catch (Exception e) {
            return Result.error("ruleConfig 序列化失败: " + e.getMessage());
        }
        tpl.setStatus(1);
        tpl.setCreatedBy(SecurityUtils.getCurrentUserId());
        templateMapper.insert(tpl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", tpl.getId());
        result.put("templateName", tpl.getTemplateName());
        return Result.success(result);
    }

    @Operation(summary = "查询审批模板列表")
    @GetMapping("/list")
    public Result<List<WfApprovalTemplate>> list(
            @RequestParam(required = false) String processKey) {
        List<WfApprovalTemplate> list;
        if (processKey != null) {
            list = templateMapper.selectByProcessKey(processKey);
        } else {
            list = templateMapper.selectAll();
        }
        return Result.success(list);
    }

    @Operation(summary = "查看模板详情")
    @GetMapping("/{id}")
    public Result<WfApprovalTemplate> getById(@PathVariable String id) {
        return Result.success(templateMapper.selectById(id));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        WfApprovalTemplate tpl = templateMapper.selectById(id);
        if (tpl == null) {
            return Result.error("模板不存在");
        }
        if (body.containsKey("templateName")) tpl.setTemplateName((String) body.get("templateName"));
        if (body.containsKey("ruleType")) tpl.setRuleType((String) body.get("ruleType"));
        if (body.containsKey("ruleConfig")) {
            try {
                tpl.setRuleConfig(objectMapper.writeValueAsString(body.get("ruleConfig")));
            } catch (Exception e) {
                return Result.error("ruleConfig 序列化失败: " + e.getMessage());
            }
        }
        templateMapper.update(tpl);
        return Result.success();
    }

    @Operation(summary = "禁用/启用模板")
    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable String id, @RequestParam int status) {
        templateMapper.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        templateMapper.deleteById(id);
        return Result.success();
    }
}
