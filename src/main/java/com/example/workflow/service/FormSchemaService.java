package com.example.workflow.service;

import com.example.workflow.dto.CreateFormSchemaRequest;
import com.example.workflow.entity.WfFormSchema;
import com.example.workflow.mapper.FormSchemaMapper;
import com.example.workflow.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * 表单Schema服务 — JSON Schema定义 + UI渲染配置 + 字段级控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormSchemaService {

    private final FormSchemaMapper formSchemaMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String createFormSchema(CreateFormSchemaRequest req) {
        validateJsonSchema(req.getJsonSchema());

        WfFormSchema schema = new WfFormSchema();
        schema.setId(UUID.randomUUID().toString().replace("-", ""));
        schema.setSchemaName(req.getSchemaName());
        schema.setSchemaKey(req.getSchemaKey());
        schema.setSchemaVersion(req.getSchemaVersion() != null ? req.getSchemaVersion() : 1);
        schema.setJsonSchema(req.getJsonSchema());
        schema.setUiSchema(req.getUiSchema());
        schema.setFieldsConfig(req.getFieldsConfig());
        schema.setApplicableOrgs(req.getApplicableOrgs());
        schema.setStatus(1);
        schema.setCreatedBy(SecurityUtils.getCurrentUserId());

        formSchemaMapper.insert(schema);
        log.info("表单Schema创建成功: id={}, key={}", schema.getId(), schema.getSchemaKey());
        return schema.getId();
    }

    @Transactional
    public void updateFormSchema(String id, CreateFormSchemaRequest req) {
        WfFormSchema existing = formSchemaMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("表单Schema不存在: " + id);
        validateJsonSchema(req.getJsonSchema());
        existing.setSchemaName(req.getSchemaName());
        existing.setJsonSchema(req.getJsonSchema());
        existing.setUiSchema(req.getUiSchema());
        existing.setFieldsConfig(req.getFieldsConfig());
        formSchemaMapper.update(id, existing);
        log.info("表单Schema更新成功: id={}", id);
    }

    @Transactional
    public void deleteFormSchema(String id) {
        formSchemaMapper.deleteById(id);
        log.info("表单Schema删除成功: id={}", id);
    }

    public WfFormSchema getFormSchemaById(String id) {
        return formSchemaMapper.selectById(id);
    }

    public WfFormSchema getFormSchemaByKey(String key) {
        return formSchemaMapper.selectByKey(key);
    }

    public List<WfFormSchema> listFormSchemas(Integer status, String schemaKey) {
        return formSchemaMapper.selectList(status, schemaKey);
    }

    /**
     * 验证业务数据是否符合JSON Schema（使用draft-07）
     */
    public List<String> validateFormData(String jsonSchema, String businessData) {
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchema);
            JsonNode dataNode = objectMapper.readTree(businessData);

            // JSON Schema 验证
            com.networknt.schema.JsonSchemaFactory factory =
                    com.networknt.schema.JsonSchemaFactory.getInstance(
                            com.networknt.schema.SpecVersionDetector.detect(schemaNode));
            com.networknt.schema.JsonSchema schema = factory.getSchema(schemaNode);
            java.util.Set<com.networknt.schema.ValidationMessage> errors = schema.validate(dataNode);
            if (errors.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> msgs = new ArrayList<>();
            for (com.networknt.schema.ValidationMessage em : errors) {
                msgs.add(em.getMessage());
            }
            return msgs;
        } catch (Exception e) {
            log.error("JSON Schema验证异常", e);
            return Collections.singletonList("验证异常: " + e.getMessage());
        }
    }

    private void validateJsonSchema(String jsonSchemaStr) {
        try {
            objectMapper.readTree(jsonSchemaStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON Schema格式错误: " + e.getMessage());
        }
    }

    /**
     * 从JSON Schema提取字段列表（用于前端动态渲染）
     */
    public List<Map<String, Object>> extractFields(String jsonSchema) {
        try {
            JsonNode root = objectMapper.readTree(jsonSchema);
            JsonNode properties = root.get("properties");
            if (properties == null) return Collections.emptyList();

            List<Map<String, Object>> fields = new ArrayList<>();
            properties.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldDef = properties.get(fieldName);
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("name", fieldName);
                field.put("type", fieldDef.has("type") ? fieldDef.get("type").asText() : "string");
                field.put("title", fieldDef.has("title") ? fieldDef.get("title").asText() : fieldName);
                field.put("required", isRequired(root, fieldName));
                field.put("widget", fieldDef.has("widget") ? fieldDef.get("widget").asText() : "text");
                if (fieldDef.has("minimum")) field.put("minimum", fieldDef.get("minimum").asInt());
                if (fieldDef.has("maximum")) field.put("maximum", fieldDef.get("maximum").asInt());
                if (fieldDef.has("maxLength")) field.put("maxLength", fieldDef.get("maxLength").asInt());
                if (fieldDef.has("enum")) {
                    field.put("options", StreamSupport.stream(fieldDef.get("enum").spliterator(), false)
                            .map(n -> n.asText()).toList());
                }
                fields.add(field);
            });
            return fields;
        } catch (Exception e) {
            log.error("提取字段失败", e);
            return Collections.emptyList();
        }
    }

    private boolean isRequired(JsonNode schema, String fieldName) {
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode item : required) {
                if (fieldName.equals(item.asText())) return true;
            }
        }
        return false;
    }
}
