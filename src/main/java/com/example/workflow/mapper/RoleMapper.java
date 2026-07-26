package com.example.workflow.mapper;

import com.example.workflow.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    SysRole selectById(@Param("id") String id);

    SysRole selectByCode(@Param("code") String code);

    /** 创建角色 */
    int insert(SysRole role);

    /** 更新角色 */
    int update(SysRole role);

    /** 删除角色 */
    int deleteById(@Param("id") String id);

    /** 查询所有角色 */
    List<SysRole> selectAll();
}
