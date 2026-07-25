package com.example.workflow.mapper;

import com.example.workflow.entity.WfProcessDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProcessDefMapper {
    void insert(WfProcessDef def);

    WfProcessDef selectById(@Param("id") String id);

    WfProcessDef selectByProcessKeyAndVersion(@Param("key") String key, @Param("version") Integer version);

    List<WfProcessDef> selectList(@Param("status") Integer status, @Param("processKey") String processKey);

    void updateStatus(@Param("id") String id, @Param("status") Integer status);
}
