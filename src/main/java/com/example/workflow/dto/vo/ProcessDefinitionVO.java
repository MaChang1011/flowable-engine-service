package com.example.workflow.dto.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProcessDefinitionVO {
    private String id;
    private String key;
    private String name;
    private Integer version;
    private String category;
    private String deploymentId;
    private String bpmnXml;
    private String applicableOrgs;
    private String formSchemaId;
    private Boolean suspended;
    private List<Map<String, Object>> tasks;
}
