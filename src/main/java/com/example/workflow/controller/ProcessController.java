package com.example.workflow.controller;

import com.example.workflow.dto.DeployProcessRequest;
import com.example.workflow.dto.Result;
import com.example.workflow.entity.WfProcessDef;
import com.example.workflow.mapper.ProcessDefMapper;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.FormSchemaService;
import com.example.workflow.service.WorkflowFacade;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程定义控制器
 */
@RestController
@RequestMapping("/api/wf/process")
@RequiredArgsConstructor
public class ProcessController {

    private final RepositoryService repositoryService;
    private final WorkflowFacade workflowFacade;
    private final FormSchemaService formSchemaService;
    private final ProcessDefMapper processDefMapper;

    @PostMapping("/define/deploy")
    public Result<Map<String, Object>> deployProcess(@RequestBody DeployProcessRequest req) {
        Deployment deployment = repositoryService.createDeployment()
                .addString(req.getProcessKey() + ".bpmn20.xml", req.getBpmnXml())
                .name(req.getProcessName())
                .category(StringUtils.hasText(req.getCategory()) ? req.getCategory() : "default")
                .tenantId("default")
                .deploy();

        WfProcessDef def = new WfProcessDef();
        def.setId(UUID.randomUUID().toString().replace("-", ""));
        def.setProcessKey(req.getProcessKey());
        def.setProcessName(req.getProcessName());
        def.setVersion(1);
        def.setCategory(req.getCategory());
        def.setBpmnXml(req.getBpmnXml());
        def.setApplicableOrgs(req.getApplicableOrgs());
        def.setFormSchemaId(req.getFormSchemaId());
        def.setStatus(1);
        def.setDeployedBy(SecurityUtils.getCurrentUserId());
        processDefMapper.insert(def);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deploymentId", deployment.getId());
        result.put("processDefId", def.getId());
        return Result.success(result);
    }

    @GetMapping("/define/list")
    public Result<List<Map<String, Object>>> listProcessDefinitions(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String processKey) {
        List<WfProcessDef> defs = processDefMapper.selectList(status, processKey);
        List<Map<String, Object>> result = defs.stream().map(def -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", def.getId());
            map.put("processKey", def.getProcessKey());
            map.put("processName", def.getProcessName());
            map.put("version", def.getVersion());
            map.put("category", def.getCategory());
            map.put("formSchemaId", def.getFormSchemaId());
            map.put("status", def.getStatus());
            map.put("createdAt", def.getCreatedAt());
            List<ProcessDefinition> flowableDefs = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(def.getProcessKey()).latestVersion().list();
            if (!flowableDefs.isEmpty()) {
                ProcessDefinition fd = flowableDefs.get(0);
                map.put("flowableDeploymentId", fd.getDeploymentId());
                map.put("flowableSuspended", fd.isSuspended());
            }
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping("/define/{id}/activate")
    public Result<Void> activate(@PathVariable String id) {
        processDefMapper.updateStatus(id, 1);
        return Result.success();
    }

    @PostMapping("/define/{id}/suspend")
    public Result<Void> suspend(@PathVariable String id) {
        processDefMapper.updateStatus(id, 0);
        return Result.success();
    }

    @GetMapping("/define/{id}/fields")
    public Result<List<Map<String, Object>>> getFormFields(@PathVariable String id) {
        WfProcessDef def = processDefMapper.selectById(id);
        if (def == null || !StringUtils.hasText(def.getFormSchemaId())) {
            return Result.success(Collections.emptyList());
        }
        var formSchema = formSchemaService.getFormSchemaById(def.getFormSchemaId());
        if (formSchema == null) return Result.success(Collections.emptyList());
        return Result.success(formSchemaService.extractFields(formSchema.getJsonSchema()));
    }
}
