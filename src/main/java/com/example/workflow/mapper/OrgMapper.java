package com.example.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface OrgMapper {
    /**
     * 递归查询当前租户及所有下级的 tenantId
     */
    List<String> selectDescendantTenantIds(String currentTenantId);
}
