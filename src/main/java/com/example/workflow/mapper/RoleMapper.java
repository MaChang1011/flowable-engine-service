package com.example.workflow.mapper;

import com.example.workflow.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper {
    SysRole selectById(@Param("id") String id);

    SysRole selectByCode(@Param("code") String code);
}
