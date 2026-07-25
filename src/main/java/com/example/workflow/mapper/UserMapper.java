package com.example.workflow.mapper;

import com.example.workflow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    SysUser selectById(@Param("id") String id);

    SysUser selectByUsername(@Param("username") String username);

    String selectOrgIdById(@Param("userId") String userId);
}
