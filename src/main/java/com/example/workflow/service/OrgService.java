package com.example.workflow.service;

import com.example.workflow.dto.vo.OrgTreeNodeVO;
import com.example.workflow.entity.SysOrg;
import com.example.workflow.mapper.OrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;

    /**
     * 获取当前用户及其所有下级的机构ID列表
     */
    public List<String> getAccessibleOrgIds(String currentOrgId) {
        if (!StringUtils.hasText(currentOrgId)) {
            return Collections.emptyList();
        }
        return orgMapper.selectDescendantOrgIds(currentOrgId);
    }

    /**
     * 获取直接下级机构ID（不含自身）
     */
    public List<String> getDirectChildrenIds(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return Collections.emptyList();
        }
        return orgMapper.selectDirectChildren(orgId);
    }

    /**
     * 构建机构树（递归）
     */
    public List<OrgTreeNodeVO> buildOrgTree() {
        List<SysOrg> rootOrgs = orgMapper.selectRootOrgs();
        List<OrgTreeNodeVO> tree = new ArrayList<>();
        for (SysOrg root : rootOrgs) {
            tree.add(buildNode(root));
        }
        return tree;
    }

    /**
     * 构建单个节点的子树
     */
    private OrgTreeNodeVO buildNode(SysOrg org) {
        OrgTreeNodeVO node = new OrgTreeNodeVO();
        node.setId(org.getId());
        node.setOrgName(org.getOrgName());
        node.setParentId(org.getParentId());
        node.setOrgLevel(org.getOrgLevel());
        node.setOrgType(org.getOrgType());
        node.setOrgCode(org.getOrgCode());
        node.setSortOrder(org.getSortOrder());
        node.setStatus(org.getStatus());

        List<SysOrg> children = orgMapper.selectByParentId(org.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream().map(this::buildNode).collect(Collectors.toList()));
        } else {
            node.setChildren(Collections.emptyList());
        }
        return node;
    }

    /**
     * 根据ID查询机构详情
     */
    public SysOrg getOrgById(String orgId) {
        return orgMapper.selectById(orgId);
    }

    /**
     * 获取某机构的所有子孙节点
     */
    public List<SysOrg> getAllDescendants(String orgId) {
        return orgMapper.selectAllDescendants(orgId);
    }
}
