package com.example.workflow.controller;

import com.example.workflow.dto.Result;
import com.example.workflow.service.CommitteeVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 审批委员会控制器 — 多人投票
 */
@Tag(name = "审批委员会", description = "多人投票上会机制")
@RestController
@RequestMapping("/api/wf/committee")
@RequiredArgsConstructor
public class CommitteeController {

    private final CommitteeVoteService committeeService;

    @Operation(summary = "初始化委员会", description = "为审批节点创建委员会投票记录")
    @PostMapping("/init")
    public Result<Void> init(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> memberIds = (List<String>) body.get("memberIds");
        committeeService.initCommittee(
                (String) body.get("taskId"),
                (String) body.get("committeeName"),
                memberIds);
        return Result.success();
    }

    @Operation(summary = "委员投票", description = "委员提交投票 APPROVE/REJECT/ABSTAIN")
    @PostMapping("/vote")
    public Result<Map<String, Object>> vote(@RequestBody Map<String, Object> body) {
        double threshold = body.get("threshold") != null
                ? ((Number) body.get("threshold")).doubleValue()
                : 0.5;
        return Result.success(committeeService.castVote(
                (String) body.get("taskId"),
                (String) body.get("memberId"),
                (String) body.get("vote"),
                (String) body.getOrDefault("comment", "").toString(),
                threshold));
    }

    @Operation(summary = "查询投票统计")
    @GetMapping("/{taskId}/tally")
    public Result<Map<String, Object>> tally(@PathVariable String taskId,
                                              @RequestParam(defaultValue = "0.5") String threshold) {
        return Result.success(committeeService.tallyVotes(taskId, Double.parseDouble(threshold)));
    }

    @Operation(summary = "查询投票详情")
    @GetMapping("/{taskId}/details")
    public Result<List<Map<String, Object>>> details(@PathVariable String taskId) {
        return Result.success(committeeService.getVoteDetails(taskId));
    }
}
