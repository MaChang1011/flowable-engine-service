package com.example.workflow.controller;

import com.example.workflow.dto.CompleteTaskRequest;
import com.example.workflow.dto.RejectTaskRequest;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.entity.WfFormSchema;
import com.example.workflow.entity.WfProcessDef;
import com.example.workflow.mapper.ProcessDefMapper;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.FormSchemaService;
import com.example.workflow.service.TaskRoutingService;
import com.example.workflow.service.WorkflowFacade;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import com.example.workflow.dto.Result;

import java.util.*;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/api/wf/task")
@RequiredArgsConstructor
public class TaskController {

    private final WorkflowFacade workflowFacade;
    private final TaskRoutingService taskRoutingService;
    private final ProcessDefMapper processDefMapper;
    private final FormSchemaService formSchemaService;
    private final TaskService taskService;

    @GetMapping("/todo")
    public Result<List<TaskVO>> todoTasks(
            @RequestParam(value = "userId", required = false) String userId) {
        String currentUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        List<TaskVO> tasks = workflowFacade.getTodoTasks(currentUserId);
        return Result.success(tasks);
    }

    @GetMapping("/done")
    public Result<List<Map<String, Object>>> doneTasks(
            @RequestParam(value = "userId", required = false) String userId) {
        String currentUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> tasks = workflowFacade.getDoneTasks(currentUserId);
        return Result.success(tasks);
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody CompleteTaskRequest req) {
        workflowFacade.submitTask(req);
        return Result.success();
    }

    @PostMapping("/reject-to-node")
    public Result<Void> rejectToNode(@RequestBody RejectTaskRequest req) {
        workflowFacade.rejectToNode(req.getTaskId(), req.getTargetNodeId(), req.getVariables());
        return Result.success();
    }

    @PostMapping("/delegate")
    public Result<Void> delegate(@RequestBody Map<String, String> body) {
        taskRoutingService.delegateTask(body.get("taskId"), body.get("newAssignee"), body.getOrDefault("comment", ""));
        return Result.success();
    }

    @PostMapping("/claim")
    public Result<Void> claim(@RequestBody Map<String, String> body) {
        taskRoutingService.claimTask(body.get("taskId"), body.get("userId"));
        return Result.success();
    }

    @PostMapping("/assign")
    public Result<Void> assign(@RequestBody Map<String, String> body) {
        taskRoutingService.assignTaskToUser(body.get("taskId"), body.get("userId"), body.get("orgId"));
        return Result.success();
    }

    /**
     * 根据任务ID获取关联的表单Schema（用于动态表单渲染）
     */
    @GetMapping("/{taskId}/form-schema")
    public Result<Map<String, Object>> getTaskFormSchema(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) return Result.error("任务不存在: " + taskId);

        String processDefinitionKey = task.getProcessDefinitionId();
        // Flowable processDefinitionId 格式: "leave:1:xxx"，需要提取 key 部分
        if (processDefinitionKey != null && processDefinitionKey.contains(":")) {
            processDefinitionKey = processDefinitionKey.split(":")[0];
        }

        // 从自定义流程定义表查找 formSchemaId
        List<WfProcessDef> defs = processDefMapper.selectList(1, processDefinitionKey);
        WfProcessDef def = defs.isEmpty() ? null : defs.get(0);
        if (def == null || !org.springframework.util.StringUtils.hasText(def.getFormSchemaId())) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hasSchema", false);
            result.put("fields", Collections.emptyList());
            result.put("message", "该流程未配置表单");
            return Result.success(result);
        }

        WfFormSchema schema = formSchemaService.getFormSchemaById(def.getFormSchemaId());
        if (schema == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hasSchema", false);
            result.put("fields", Collections.emptyList());
            result.put("message", "表单Schema不存在");
            return Result.success(result);
        }

        List<Map<String, Object>> fields = formSchemaService.extractFields(schema.getJsonSchema());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasSchema", true);
        result.put("schemaId", schema.getId());
        result.put("schemaName", schema.getSchemaName());
        result.put("jsonSchema", schema.getJsonSchema());
        result.put("uiSchema", schema.getUiSchema());
        result.put("fields", fields);
        return Result.success(result);
    }
}
