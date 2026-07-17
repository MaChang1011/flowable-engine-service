package com.example.workflow.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从 HTTP Header 获取当前登录用户/租户信息
 * 
 * 实际项目中应替换为 Spring Security / JWT 解析
 * 这里通过 Header 传参演示：X-Tenant-Id: ORG_TENANT_001
 */
public class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentTenantId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("X-Tenant-Id");
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCurrentUserId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("X-User-Id");
        } catch (Exception e) {
            return null;
        }
    }
}
