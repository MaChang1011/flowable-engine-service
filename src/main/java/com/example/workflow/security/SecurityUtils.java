package com.example.workflow.security;

import lombok.Data;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentHeader(String headerName) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader(headerName);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCurrentUserId() {
        return getCurrentHeader("X-User-Id");
    }

    public static String getCurrentUsername() {
        return getCurrentHeader("X-Username");
    }

    public static String getCurrentRealName() {
        return getCurrentHeader("X-Real-Name");
    }

    public static String getCurrentOrgId() {
        return getCurrentHeader("X-Org-Id");
    }

    public static String getCurrentRoleIds() {
        return getCurrentHeader("X-Role-Ids");
    }

    public static String getCurrentTenantId() {
        return getCurrentHeader("X-Tenant-Id");
    }
}
