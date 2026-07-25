package com.example.workflow.mapper;

import com.example.workflow.entity.WfApprovalTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApprovalTemplateMapper {

    WfApprovalTemplate selectById(@Param("id") String id);

    List<WfApprovalTemplate> selectByProcessKey(@Param("processKey") String processKey);

    List<WfApprovalTemplate> selectAll();

    int insert(WfApprovalTemplate template);

    int update(WfApprovalTemplate template);

    int updateStatus(@Param("id") String id, @Param("status") Integer status);

    int deleteById(@Param("id") String id);
}
