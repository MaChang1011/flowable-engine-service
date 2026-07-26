package com.example.workflow.mapper;

import com.example.workflow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    SysUser selectById(@Param("id") String id);

    SysUser selectByUsername(@Param("username") String username);

    String selectOrgIdById(@Param("userId") String userId);

    int updateOrg(@Param("userId") String userId, @Param("orgId") String orgId);

    int updateStatus(@Param("userId") String userId, @Param("status") Integer status);

    List<SysUser> selectByOrgId(@Param("orgId") String orgId);

    SysUser selectByIdIncludeDisabled(@Param("userId") String userId);

    /** 登录用：按用户名查用户（含密码） */
    SysUser selectByUsernameWithPassword(@Param("username") String username);

    /** 创建用户 */
    int insert(SysUser user);

    /** 更新用户 */
    int update(SysUser user);

    /** 删除用户（物理删除） */
    int deleteById(@Param("id") String id);

    /** 分页查询用户列表 */
    List<SysUser> selectAll();
}
