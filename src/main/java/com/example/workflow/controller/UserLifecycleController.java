package com.example.workflow.controller;

import com.example.workflow.dto.ResignationRequest;
import com.example.workflow.dto.Result;
import com.example.workflow.dto.TransferRequest;
import com.example.workflow.service.UserLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户生命周期控制器 — 调岗 / 离职
 */
@Tag(name = "用户生命周期", description = "调岗、离职处理")
@RestController
@RequestMapping("/api/wf/user")
@RequiredArgsConstructor
public class UserLifecycleController {

    private final UserLifecycleService lifecycleService;

    @Operation(summary = "调岗", description = "将用户转移到新机构，现有待办任务保留")
    @PostMapping("/transfer")
    public Result<Map<String, Object>> transfer(@RequestBody TransferRequest req) {
        return Result.success(lifecycleService.transfer(req));
    }

    @Operation(summary = "离职", description = "禁用用户并将所有待办任务重分配给承接人")
    @PostMapping("/resign")
    public Result<Map<String, Object>> resign(@RequestBody ResignationRequest req) {
        return Result.success(lifecycleService.resign(req));
    }

    @Operation(summary = "用户任务摘要", description = "查询用户的待办任务数及基本信息")
    @GetMapping("/{userId}/summary")
    public Result<Map<String, Object>> getUserSummary(@PathVariable String userId) {
        return Result.success(lifecycleService.getUserTaskSummary(userId));
    }
}
