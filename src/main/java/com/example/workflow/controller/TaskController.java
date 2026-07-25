package com.example.workflow.controller;

import com.example.workflow.dto.CompleteTaskRequest;
import com.example.workflow.dto.RejectTaskRequest;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.TaskRoutingService;
import com.example.workflow.service.WorkflowFacade;
import lombok.RequiredArgsConstructor;
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
}
