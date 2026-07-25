package com.example.workflow.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 权限上下文 — ThreadLocal，每次HTTP请求初始化
 */
@Data
@Slf4j
@Component
public class PermissionContext {

    private static final ThreadLocal<List<String>> ACCESSIBLE_ORG_IDS = new ThreadLocal<>();
    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    public static void setAccessibleOrgIds(List<String> orgIds) {
        ACCESSIBLE_ORG_IDS.set(orgIds);
    }

    public static List<String> getAccessibleOrgIds() {
        return ACCESSIBLE_ORG_IDS.get();
    }

    public static void setCurrentUser(UserInfo user) {
        CURRENT_USER.set(user);
    }

    public static UserInfo getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        ACCESSIBLE_ORG_IDS.remove();
        CURRENT_USER.remove();
    }

    /**
     * 判断当前用户是否有跨机构权限
     */
    public static boolean isCrossOrgUser() {
        UserInfo user = CURRENT_USER.get();
        return user != null && "CROSS".equals(user.getScopeType());
    }
}
