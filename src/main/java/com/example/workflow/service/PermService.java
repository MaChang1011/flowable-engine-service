package com.example.workflow.service;

import com.example.workflow.entity.SysRole;
import com.example.workflow.mapper.OrgMapper;
import com.example.workflow.mapper.RoleMapper;
import com.example.workflow.security.PermissionContext;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.security.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermService {

    private final OrgMapper orgMapper;
    private final RoleMapper roleMapper;

    /**
     * 初始化当前请求的权限上下文
     * 在拦截器中调用，解析用户角色并计算可访问机构列表
     */
    public void initPermissionContext() {
        String userId = SecurityUtils.getCurrentUserId();
        String orgId = SecurityUtils.getCurrentOrgId();
        String roleIds = SecurityUtils.getCurrentRoleIds();

        if (!StringUtils.hasText(userId)) {
            log.warn("未找到用户信息，使用默认SELF权限");
            PermissionContext.setCurrentUser(buildDefaultUserInfo(orgId));
            return;
        }

        // 取第一个角色的scope_type作为权限范围
        List<String> roleIdList = StringUtils.hasText(roleIds) ? Arrays.asList(roleIds.split(",")) : Collections.emptyList();
        String scopeType = "SELF";
        String scopeOrgIds = null;

        for (String roleId : roleIdList) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null) {
                scopeType = role.getScopeType();
                scopeOrgIds = role.getScopeOrgIds();
                break;
            }
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(SecurityUtils.getCurrentUsername());
        userInfo.setRealName(SecurityUtils.getCurrentRealName());
        userInfo.setOrgId(orgId);
        userInfo.setRoleIds(roleIds);
        userInfo.setScopeType(scopeType);
        userInfo.setScopeOrgIds(scopeOrgIds);

        PermissionContext.setCurrentUser(userInfo);

        // 计算可访问的机构ID列表
        List<String> accessibleOrgIds = calculateAccessibleOrgIds(orgId, scopeType, scopeOrgIds);
        PermissionContext.setAccessibleOrgIds(accessibleOrgIds);
    }

    /**
     * 根据角色scope_type计算可访问的机构ID列表
     */
    private List<String> calculateAccessibleOrgIds(String currentOrgId, String scopeType, String scopeOrgIds) {
        if (!StringUtils.hasText(currentOrgId)) {
            return Collections.emptyList();
        }

        return switch (scopeType) {
            case "SELF" -> List.of(currentOrgId);

            case "DEPT" -> orgMapper.selectDescendantOrgIds(currentOrgId);

            case "ALL" -> orgMapper.selectDirectChildren(currentOrgId);

            case "CROSS" -> {
                if (StringUtils.hasText(scopeOrgIds)) {
                    yield Arrays.stream(scopeOrgIds.split(",")).map(String::trim).collect(Collectors.toList());
                }
                yield Collections.singletonList(currentOrgId);
            }

            default -> List.of(currentOrgId);
        };
    }

    private UserInfo buildDefaultUserInfo(String orgId) {
        UserInfo info = new UserInfo();
        info.setUserId(SecurityUtils.getCurrentUserId());
        info.setOrgId(orgId);
        info.setScopeType("SELF");
        return info;
    }

    /**
     * 获取当前用户可访问的机构ID列表（直接调用）
     */
    public List<String> getAccessibleOrgIds() {
        return PermissionContext.getAccessibleOrgIds();
    }

    /**
     * 根据参数直接计算可访问机构列表（供 AuthService 调用）
     */
    public List<String> calculateAccessibleOrgIdsDirect(String currentOrgId, String scopeType, String scopeOrgIds) {
        return calculateAccessibleOrgIds(currentOrgId, scopeType, scopeOrgIds);
    }

    /**
     * 判断目标机构是否在用户权限范围内
     */
    public boolean isOrgAccessible(String targetOrgId) {
        List<String> accessible = PermissionContext.getAccessibleOrgIds();
        return accessible != null && accessible.contains(targetOrgId);
    }
}
