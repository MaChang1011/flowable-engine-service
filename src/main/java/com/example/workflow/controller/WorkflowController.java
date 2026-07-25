package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.dto.vo.ProcessInstanceVO;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.FlowableQueryHelper;
import com.example.workflow.service.WorkflowFacade;
import com.example.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流统一控制器 —— 业务系统的唯一入口
 * 
 * 所有流程操作都通过此接口完成：
 *   启动、提交、驳回、查询待办/已办、流程追踪
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowFacade workflowFacade;
    private final FlowableQueryHelper queryHelper;

    // ==================== 流程启动 ====================

    /**
     * 启动流程实例
     * 
     * POST /api/workflow/start
     * {
     *   "processDefinitionKey": "leave_request",
     *   "businessKey": "ORDER_001",
     *   "tenantId": "ORG_TENANT_001",
     *   "variables": { "days": 3, "applicant": "张三" }
     * }
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody StartProcessRequest req) {
        Map<String, Object> result = workflowFacade.startProcess(req);
        return Result.success(result);
    }

    // ==================== 任务处理 ====================

    /**
     * 提交任务（审批通过/驳回）
     * 
     * POST /api/workflow/submit
     * {
     *   "taskId": "task-12345",
     *   "variables": { "approved": true },
     *   "comment": "同意"
     * }
     */
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody CompleteTaskRequest req) {
        workflowFacade.submitTask(req);
        return Result.success();
    }

    /**
     * 驳回到指定节点
     * 
     * POST /api/workflow/reject
     * {
     *   "taskId": "task-12345",
     *   "targetNodeId": "apply",          // 驳回到哪个节点
     *   "variables": { "rejected": true },
     *   "comment": "材料不全，请补充"
     * }
     */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody RejectTaskRequest req) {
        workflowService.rejectToNode(
                req.getTaskId(),
                req.getTargetNodeId(),
                req.getVariables()
        );
        return Result.success();
    }

    /**
     * 驳回到流程开始
     * 
     * POST /api/workflow/reject-to-start
     * {
     *   "taskId": "task-12345",
     *   "comment": "重新填写"
     * }
     */
    @PostMapping("/reject-to-start")
    public Result<Void> rejectToStart(@RequestBody RejectTaskRequest req) {
        workflowService.rejectToStart(req.getTaskId(), req.getVariables());
        return Result.success();
    }

    // ==================== 任务查询 ====================

    /**
     * 查询我的待办
     * 
     * GET /api/workflow/todo?userId=张三
     */
    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> todoTasks(
            @RequestParam(value = "userId", required = false) String userId) {
        
        String currentUserId = StringUtils.hasText(userId) 
                ? userId 
                : SecurityUtils.getCurrentUserId();
        
        List<Map<String, Object>> tasks = workflowService.getTodoTasks(currentUserId);
        return Result.success(tasks);
    }

    /**
     * 查询我的已办
     * 
     * GET /api/workflow/done?userId=张三
     */
    @GetMapping("/done")
    public Result<List<Map<String, Object>>> doneTasks(
            @RequestParam(value = "userId", required = false) String userId) {
        
        String currentUserId = StringUtils.hasText(userId)
                ? userId
                : SecurityUtils.getCurrentUserId();
        
        List<Map<String, Object>> tasks = workflowService.getDoneTasks(currentUserId);
        return Result.success(tasks);
    }

    // ==================== 流程追踪 ====================

    /**
     * 查询流程实例详情
     * 
     * GET /api/workflow/instance/{processInstanceId}
     */
    @GetMapping("/instance/{processInstanceId}")
    public Result<Map<String, Object>> getInstanceDetail(
            @PathVariable String processInstanceId) {
        Map<String, Object> detail = workflowService.getProcessInstanceDetail(processInstanceId);
        return Result.success(detail);
    }

    /**
     * 获取流程轨迹（已执行的活动列表）
     * 
     * GET /api/workflow/trace/{processInstanceId}
     */
    @GetMapping("/trace/{processInstanceId}")
    public Result<List<Map<String, Object>>> getTrace(
            @PathVariable String processInstanceId) {
        List<Map<String, Object>> trace = workflowService.getProcessTrace(processInstanceId);
        return Result.success(trace);
    }

    /**
     * 获取当前活跃节点（未完成的步骤）
     * 
     * GET /api/workflow/active-nodes/{processInstanceId}
     */
    @GetMapping("/active-nodes/{processInstanceId}")
    public Result<List<Map<String, Object>>> getActiveNodes(
            @PathVariable String processInstanceId) {
        List<Map<String, Object>> active = workflowService.getActiveActivities(processInstanceId);
        return Result.success(active);
    }

    // ==================== 流程管理 ====================

    /**
     * 终止流程
     * 
     * POST /api/workflow/terminate/{processInstanceId}
     * { "reason": "业务取消" }
     */
    @PostMapping("/terminate/{processInstanceId}")
    public Result<Void> terminate(
            @PathVariable String processInstanceId,
            @RequestBody(required = false) Map<String, String> body) {
        
        String reason = body != null && body.containsKey("reason")
                ? body.get("reason")
                : "用户主动终止";
        
        workflowService.terminateProcess(processInstanceId, reason);
        return Result.success();
    }

    /**
     * 挂起流程
     */
    @PostMapping("/suspend/{processInstanceId}")
    public Result<Void> suspend(@PathVariable String processInstanceId) {
        workflowService.suspendProcess(processInstanceId);
        return Result.success();
    }

    /**
     * 激活流程
     */
    @PostMapping("/activate/{processInstanceId}")
    public Result<Void> activate(@PathVariable String processInstanceId) {
        workflowService.activateProcess(processInstanceId);
        return Result.success();
    }
}
