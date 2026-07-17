package com.example.workflow.service;

import com.example.workflow.mapper.OrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;

    /**
     * 获取当前用户及其所有下级的 tenantId 列表
     * 
     * @param currentTenantId 当前用户的租户ID
     * @return 可访问的 tenantId 列表（含自身及所有子孙节点）
     */
    public List<String> getAccessibleTenantIds(String currentTenantId) {
        if (!StringUtils.hasText(currentTenantId)) {
            return Collections.emptyList();
        }
        return orgMapper.selectDescendantTenantIds(currentTenantId);
    }
}
