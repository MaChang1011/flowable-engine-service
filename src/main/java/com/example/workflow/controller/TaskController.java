package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.dto.vo.TaskVO;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.FlowableQueryHelper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowableQueryHelper queryHelper;

    /**
     * 查询待办任务
     */
    @GetMapping("/todo")
    public Result<Map<String, Object>> todoTasks(
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "candidateGroup", required = false) String candidateGroup,
            @RequestParam(value = "processDefinitionKey", required = false) String procKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        String userId = StringUtils.hasText(assignee) ? assignee : SecurityUtils.getCurrentUserId();

        TaskQuery query = taskService.createTaskQuery();

        // 核心：注入租户权限
        queryHelper.applyTenantFilter(query);

        if (StringUtils.hasText(userId)) {
            // taskCandidateOrAssigned: 已分配给该用户 或 该用户在候选组/候选人中
            query.taskCandidateOrAssigned(userId);
            if (StringUtils.hasText(candidateGroup)) {
                query.taskCandidateGroup(candidateGroup);
            }
        }
        if (StringUtils.hasText(procKey)) {
            query.processDefinitionKey(procKey);
        }

        long total = query.count();
        List<Task> tasks = query.listPage((page - 1) * size, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("data", tasks.stream().map(this::buildTaskVO).collect(Collectors.toList()));
        return Result.success(result);
    }

    /**
     * 查询已办任务（历史）
     */
    @GetMapping("/done")
    public Result<Map<String, Object>> doneTasks(
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "processDefinitionKey", required = false) String procKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        var query = historyService.createHistoricTaskInstanceQuery()
                .finished();

        // 核心：注入租户权限
        queryHelper.applyTenantFilter(query);

        String userId = StringUtils.hasText(assignee) ? assignee : SecurityUtils.getCurrentUserId();
        if (StringUtils.hasText(userId)) {
            query.taskAssignee(userId);
        }
        if (StringUtils.hasText(procKey)) {
            query.processDefinitionKey(procKey);
        }

        long total = query.count();
        List<HistoricTaskInstance> tasks = query.listPage((page - 1) * size, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("data", tasks.stream().map(t -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", t.getId());
            vo.put("name", t.getName());
            vo.put("assignee", t.getAssignee());
            vo.put("processInstanceId", t.getProcessInstanceId());
            vo.put("processDefinitionId", t.getProcessDefinitionId());
            vo.put("createTime", t.getCreateTime());
            vo.put("endTime", t.getEndTime());
            vo.put("duration", t.getDurationInMillis());
            return vo;
        }).collect(Collectors.toList()));
        return Result.success(result);
    }

    /**
     * 完成任务
     */
    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody CompleteTaskRequest req) {
        Map<String, Object> variables = Optional.ofNullable(req.getVariables()).orElse(Map.of());
        
        // Flowable 7.x: complete(taskId, variablesMap)
        taskService.complete(req.getTaskId(), variables);

        if (StringUtils.hasText(req.getComment())) {
            Task task = taskService.createTaskQuery().taskId(req.getTaskId()).singleResult();
            if (task != null) {
                taskService.addComment(req.getTaskId(), task.getProcessInstanceId(), req.getComment());
            }
        }
        return Result.success();
    }

    /**
     * 签收任务
     */
    @PostMapping("/claim")
    public Result<Void> claim(@RequestBody ClaimTaskRequest req) {
        taskService.claim(req.getTaskId(), req.getAssignee());
        return Result.success();
    }

    /**
     * 转派任务
     */
    @PostMapping("/delegate")
    public Result<Void> delegate(@RequestBody DelegateTaskRequest req) {
        taskService.delegateTask(req.getTaskId(), req.getNewAssignee());
        return Result.success();
    }

    /**
     * 获取任务详情（含流程变量）
     */
    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> getDetail(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return Result.fail("任务不存在: " + taskId);
        }

        Map<String, Object> vo = buildTaskVO(task);
        // 附带流程变量
        Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());
        vo.put("variables", vars);
        return Result.success(vo);
    }

    private Map<String, Object> buildTaskVO(Task t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", t.getId());
        vo.put("name", t.getName());
        vo.put("description", t.getDescription());
        vo.put("assignee", t.getAssignee());
        vo.put("processInstanceId", t.getProcessInstanceId());
        vo.put("processDefinitionId", t.getProcessDefinitionId());
        vo.put("tenantId", t.getTenantId());
        vo.put("createTime", t.getCreateTime());
        vo.put("dueDate", t.getDueDate());
        vo.put("state", t.getState());
        return vo;
    }
}
