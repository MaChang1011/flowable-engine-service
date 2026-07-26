package com.example.workflow.service;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.LoginResponse;
import com.example.workflow.entity.SysOrg;
import com.example.workflow.entity.SysUser;
import com.example.workflow.mapper.OrgMapper;
import com.example.workflow.mapper.RoleMapper;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.security.PermissionContext;
import com.example.workflow.security.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final OrgMapper orgMapper;
    private final RoleMapper roleMapper;
    private final PermService permService;

    // JWT 密钥（生产环境应从配置读取）
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor("workflow-secret-key-for-jwt-signing-2026-change-me-in-prod".getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION = 7 * 24 * 3600 * 1000L; // 7天

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        // 查询用户（包含已禁用的，前端根据 status 判断）
        SysUser user = userMapper.selectByUsernameWithPassword(req.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + req.getUsername());
        }

        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 密码校验（简单 MD5，生产应用 BCrypt）
        String inputHash = DigestUtils.md5DigestAsHex(req.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!inputHash.equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 查询机构名称
        SysOrg org = orgMapper.selectById(user.getOrgId());

        // 查询角色 scope
        List<String> scopeTypes = new ArrayList<>();
        if (StringUtils.hasText(user.getRoleIds())) {
            for (String roleId : user.getRoleIds().split(",")) {
                var role = roleMapper.selectById(roleId.trim());
                if (role != null && StringUtils.hasText(role.getScopeType())) {
                    scopeTypes.add(role.getScopeType());
                }
            }
        }
        String scopeType = scopeTypes.isEmpty() ? "SELF" : scopeTypes.get(0);

        // 计算可访问机构
        List<String> accessibleOrgIds = permService.calculateAccessibleOrgIdsDirect(
                user.getOrgId(), scopeType, null);

        // 生成 JWT Token (jjwt 0.12 API)
        String token = Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("orgId", user.getOrgId())
                .claim("roleIds", user.getRoleIds())
                .claim("scopeType", scopeType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(JWT_KEY)
                .compact();

        return LoginResponse.of(token, user, org != null ? org.getOrgName() : "",
                scopeType, accessibleOrgIds);
    }

    /**
     * 验证并解析 JWT Token
     */
    public Map<String, Object> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(JWT_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", claims.getSubject());
            result.put("username", claims.get("username", String.class));
            result.put("realName", claims.get("realName", String.class));
            result.put("orgId", claims.get("orgId", String.class));
            result.put("roleIds", claims.get("roleIds", String.class));
            result.put("scopeType", claims.get("scopeType", String.class));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token 无效或已过期");
        }
    }
}
