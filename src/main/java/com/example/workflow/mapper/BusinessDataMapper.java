package com.example.workflow.mapper;

import com.example.workflow.entity.WfBusinessData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessDataMapper {
    void insert(WfBusinessData data);

    WfBusinessData selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    WfBusinessData selectByBusinessKey(@Param("businessKey") String businessKey);

    void update(@Param("id") String id, @Param("data") WfBusinessData data);
}
