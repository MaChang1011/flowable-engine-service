package com.example.workflow.controller;

import com.example.workflow.dto.vo.OrgTreeNodeVO;
import com.example.workflow.entity.SysOrg;
import com.example.workflow.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.workflow.dto.Result;

import java.util.*;

/**
 * 机构权限控制器
 */
@RestController
@RequestMapping("/api/wf/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * 获取机构树（完整层级）
     */
    @GetMapping("/tree")
    public Result<List<OrgTreeNodeVO>> getOrgTree() {
        List<OrgTreeNodeVO> tree = orgService.buildOrgTree();
        return Result.success(tree);
    }

    /**
     * 查询子机构列表
     */
    @GetMapping("/{orgId}/children")
    public Result<List<SysOrg>> getChildren(@PathVariable String orgId) {
        List<SysOrg> children = orgService.getOrgById(orgId) != null 
                ? orgService.getAllDescendants(orgId).stream()
                    .filter(o -> o.getParentId().equals(orgId))
                    .toList()
                : Collections.emptyList();
        return Result.success(children);
    }

    /**
     * 查询机构详情
     */
    @GetMapping("/{orgId}")
    public Result<SysOrg> getOrgDetail(@PathVariable String orgId) {
        SysOrg org = orgService.getOrgById(orgId);
        if (org == null) {
            return Result.error("机构不存在: " + orgId);
        }
        return Result.success(org);
    }

    /**
     * 查询当前用户可访问的机构ID列表
     */
    @GetMapping("/accessible")
    public Result<List<String>> getAccessibleOrgIds() {
        // 这里需要通过PermissionContext或SecurityUtils获取
        return Result.success(Collections.singletonList("ORG_ROOT"));
    }
}
