package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.entity.SysOrg;
import com.example.workflow.entity.SysRole;
import com.example.workflow.entity.SysUser;
import com.example.workflow.mapper.OrgMapper;
import com.example.workflow.mapper.RoleMapper;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 认证与管理接口
 */
@Tag(name = "认证管理", description = "登录、用户/机构/角色管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final OrgMapper orgMapper;
    private final RoleMapper roleMapper;

    // ==================== 登录 ====================

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        try {
            LoginResponse resp = authService.login(req);
            return Result.success(resp);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 用户管理 ====================

    @Operation(summary = "查询所有用户")
    @GetMapping("/users")
    public Result<List<SysUser>> listUsers() {
        return Result.success(userMapper.selectAll());
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public Result<SysUser> createUser(@RequestBody UserCreateRequest req) {
        SysUser user = new SysUser();
        user.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(req.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(req.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setRealName(req.getRealName());
        user.setOrgId(req.getOrgId());
        user.setRoleIds(req.getRoleIds());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable String id,
                                    @RequestBody UserUpdateRequest req) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(req.getRealName());
        user.setOrgId(req.getOrgId());
        user.setRoleIds(req.getRoleIds());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus(req.getStatus());
        int rows = userMapper.update(user);
        if (rows == 0) return Result.error("用户不存在");
        return Result.success(null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable String id) {
        userMapper.deleteById(id);
        return Result.success(null);
    }

    // ==================== 机构管理 ====================

    @Operation(summary = "查询所有机构")
    @GetMapping("/orgs")
    public Result<List<SysOrg>> listOrgs() {
        return Result.success(orgMapper.selectAll());
    }

    @Operation(summary = "创建机构")
    @PostMapping("/orgs")
    public Result<SysOrg> createOrg(@RequestBody OrgCreateRequest req) {
        SysOrg org = new SysOrg();
        org.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        org.setOrgName(req.getOrgName());
        org.setParentId(req.getParentId());
        org.setSortOrder(req.getSortOrder());
        org.setStatus(1);
        
        // 自动计算 org_level 和 org_code
        if ("0".equals(req.getParentId()) || req.getParentId() == null) {
            org.setOrgLevel(1);
            org.setOrgType("GROUP");
            org.setOrgCode("GRP_" + System.currentTimeMillis());
        } else {
            SysOrg parent = orgMapper.selectById(req.getParentId());
            int parentLevel = parent != null && parent.getOrgLevel() != null ? parent.getOrgLevel() : 1;
            org.setOrgLevel(parentLevel + 1);
            org.setOrgType(parent != null && parent.getOrgType() != null ? parent.getOrgType() : "DEPT");
            org.setOrgCode(parent != null && parent.getOrgCode() != null ? parent.getOrgCode() + "_" + System.currentTimeMillis() : "GRP_" + System.currentTimeMillis());
        }
        
        orgMapper.insert(org);
        return Result.success(org);
    }

    @Operation(summary = "更新机构")
    @PutMapping("/orgs/{id}")
    public Result<Void> updateOrg(@PathVariable String id,
                                   @RequestBody OrgCreateRequest req) {
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setOrgName(req.getOrgName());
        org.setParentId(req.getParentId());
        org.setSortOrder(req.getSortOrder());
        orgMapper.update(org);
        return Result.success(null);
    }

    @Operation(summary = "删除机构")
    @DeleteMapping("/orgs/{id}")
    public Result<Void> deleteOrg(@PathVariable String id) {
        orgMapper.deleteById(id);
        return Result.success(null);
    }

    // ==================== 角色管理 ====================

    @Operation(summary = "查询所有角色")
    @GetMapping("/roles")
    public Result<List<SysRole>> listRoles() {
        return Result.success(roleMapper.selectAll());
    }

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    public Result<SysRole> createRole(@RequestBody RoleCreateRequest req) {
        SysRole role = new SysRole();
        role.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getCode());
        role.setScopeType(req.getScopeType());
        role.setScopeOrgIds(req.getScopeOrgIds());
        role.setDescription(req.getDescription());
        roleMapper.insert(role);
        return Result.success(role);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable String id,
                                    @RequestBody RoleCreateRequest req) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getCode());
        role.setScopeType(req.getScopeType());
        role.setScopeOrgIds(req.getScopeOrgIds());
        role.setDescription(req.getDescription());
        roleMapper.update(role);
        return Result.success(null);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable String id) {
        roleMapper.deleteById(id);
        return Result.success(null);
    }
}
