package com.example.workflow.mapper;

import com.example.workflow.entity.WfFormSchema;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FormSchemaMapper {
    void insert(WfFormSchema schema);

    WfFormSchema selectById(@Param("id") String id);

    WfFormSchema selectByKey(@Param("key") String key);

    List<WfFormSchema> selectList(@Param("status") Integer status, @Param("schemaKey") String schemaKey);

    void update(@Param("id") String id, @Param("schema") WfFormSchema schema);

    void deleteById(@Param("id") String id);
}
