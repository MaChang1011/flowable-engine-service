package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.service.EscalationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 时效升级控制器
 */
@Tag(name = "时效升级", description = "超时未审批自动 escalation")
@RestController
@RequestMapping("/api/wf/escalation")
@RequiredArgsConstructor
public class EscalationController {

    private final EscalationService escalationService;

    @Operation(summary = "手动触发升级")
    @PostMapping("/{taskId}")
    public Result<Map<String, Object>> escalate(@PathVariable String taskId) {
        return Result.success(escalationService.manualEscalate(taskId));
    }

    @Operation(summary = "查询超时任务")
    @GetMapping("/overdue")
    public Result<List<Map<String, Object>>> overdue() {
        return Result.success(escalationService.getOverdueTasks());
    }
}
