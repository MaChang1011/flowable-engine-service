package com.example.workflow.mapper;

import com.example.workflow.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrgMapper {
    /** 递归查询某机构及其所有下级机构ID */
    List<String> selectDescendantOrgIds(@Param("orgId") String orgId);

    /** 递归查询某机构的所有直接下级（不含自身） */
    List<String> selectDirectChildren(@Param("orgId") String orgId);

    /** 查询机构详情 */
    SysOrg selectById(@Param("id") String id);

    /** 查询所有顶级机构 */
    List<SysOrg> selectRootOrgs();

    /** 查询某机构的所有子孙节点（含层级信息） */
    List<SysOrg> selectAllDescendants(@Param("orgId") String orgId);

    /** 根据parent_id查询子机构列表 */
    List<SysOrg> selectByParentId(@Param("parentId") String parentId);
}
