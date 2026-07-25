package com.example.workflow.controller;

import com.example.workflow.dto.CompleteTaskRequest;
import com.example.workflow.dto.RejectTaskRequest;
import com.example.workflow.dto.StartProcessRequest;
import com.example.workflow.dto.vo.ProcessInstanceVO;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.service.TaskRoutingService;
import com.example.workflow.service.WorkflowFacade;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.workflow.dto.Result;

import java.util.*;

/**
 * 流程实例控制器
 */
@Tag(name = "流程实例", description = "启动、查询、终止流程实例")
@RestController
@RequestMapping("/api/wf/instance")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final WorkflowFacade workflowFacade;
    private final TaskRoutingService taskRoutingService;

    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody StartProcessRequest req) {
        Map<String, Object> result = workflowFacade.startProcess(req);
        return Result.success(result);
    }

    @GetMapping("/{processInstanceId}")
    public Result<ProcessInstanceVO> getInstanceDetail(@PathVariable String processInstanceId) {
        ProcessInstanceVO vo = workflowFacade.getProcessInstanceDetail(processInstanceId);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<List<ProcessInstanceVO>> listInstances(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProcessInstanceVO> instances = new ArrayList<>();
        return Result.success(instances);
    }

    @PostMapping("/{processInstanceId}/terminate")
    public Result<Void> terminate(@PathVariable String processInstanceId,
                                   @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.containsKey("reason") ? body.get("reason") : "用户主动终止";
        workflowFacade.terminateProcess(processInstanceId, reason);
        return Result.success();
    }

    @PostMapping("/{processInstanceId}/suspend")
    public Result<Void> suspend(@PathVariable String processInstanceId) {
        workflowFacade.suspendProcess(processInstanceId);
        return Result.success();
    }

    @PostMapping("/{processInstanceId}/activate")
    public Result<Void> activate(@PathVariable String processInstanceId) {
        workflowFacade.activateProcess(processInstanceId);
        return Result.success();
    }

    @GetMapping("/{processInstanceId}/trace")
    public Result<List<Map<String, Object>>> getTrace(@PathVariable String processInstanceId) {
        List<Map<String, Object>> trace = workflowFacade.getProcessTrace(processInstanceId);
        return Result.success(trace);
    }

    @GetMapping("/{processInstanceId}/active-nodes")
    public Result<List<Map<String, Object>>> getActiveNodes(@PathVariable String processInstanceId) {
        List<Map<String, Object>> active = workflowFacade.getActiveActivities(processInstanceId);
        return Result.success(active);
    }
}
