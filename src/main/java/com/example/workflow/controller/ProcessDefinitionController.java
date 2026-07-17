package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.dto.vo.ProcessDefinitionVO;
import com.example.workflow.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process/definition")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final RepositoryService repositoryService;
    private final DeploymentService deploymentService;

    /**
     * 上传部署流程定义
     */
    @PostMapping("/deploy")
    public Result<String> deploy(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "tenantId", required = false) String tenantId) {
        try {
            String deploymentId = deploymentService.deploy(file, tenantId);
            return Result.success(deploymentId);
        } catch (IOException e) {
            return Result.fail("部署失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程定义列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "latest", defaultValue = "true") boolean latest,
            @RequestParam(value = "suspended", defaultValue = "false") boolean suspended,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        var query = repositoryService.createProcessDefinitionQuery();

        if (StringUtils.hasText(key)) {
            query.processDefinitionKey(key);
        }
        if (StringUtils.hasText(name)) {
            query.processDefinitionNameLike("%" + name + "%");
        }
        if (StringUtils.hasText(tenantId)) {
            query.processDefinitionTenantId(tenantId);
        }
        if (latest) {
            query.latestVersion();
        }
        if (suspended) {
            query.suspended();
        } else {
            query.active();
        }

        long total = query.count();
        List<ProcessDefinition> defs = query.listPage((page - 1) * size, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("data", defs.stream().map(this::toVO).collect(Collectors.toList()));
        return Result.success(result);
    }

    @PostMapping("/{definitionId}/suspend")
    public Result<Void> suspend(@PathVariable String definitionId) {
        repositoryService.suspendProcessDefinitionById(definitionId, true, null);
        return Result.success();
    }

    @PostMapping("/{definitionId}/activate")
    public Result<Void> activate(@PathVariable String definitionId) {
        repositoryService.activateProcessDefinitionById(definitionId, true, null);
        return Result.success();
    }

    @DeleteMapping("/{deploymentId}")
    public Result<Void> delete(@PathVariable String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
        return Result.success();
    }

    private ProcessDefinitionVO toVO(ProcessDefinition def) {
        return new ProcessDefinitionVO(
            def.getId(), def.getKey(), def.getName(),
            def.getVersion(), def.getDeploymentId(),
            def.getResourceName(), def.getCategory(),
            def.isSuspended(), def.getTenantId()
        );
    }
}
