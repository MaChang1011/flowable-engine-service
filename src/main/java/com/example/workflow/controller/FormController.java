package com.example.workflow.controller;

import com.example.workflow.dto.CreateFormSchemaRequest;
import com.example.workflow.entity.WfFormSchema;
import com.example.workflow.service.FormSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.workflow.dto.Result;

import java.util.*;

/**
 * 表单设计器控制器
 */
@RestController
@RequestMapping("/api/wf/form")
@RequiredArgsConstructor
public class FormController {

    private final FormSchemaService formSchemaService;

    @PostMapping("/schema")
    public Result<String> createFormSchema(@RequestBody CreateFormSchemaRequest req) {
        String id = formSchemaService.createFormSchema(req);
        return Result.success(id);
    }

    @PutMapping("/schema/{id}")
    public Result<Void> updateFormSchema(@PathVariable String id, 
                                          @RequestBody CreateFormSchemaRequest req) {
        formSchemaService.updateFormSchema(id, req);
        return Result.success();
    }

    @DeleteMapping("/schema/{id}")
    public Result<Void> deleteFormSchema(@PathVariable String id) {
        formSchemaService.deleteFormSchema(id);
        return Result.success();
    }

    @GetMapping("/schema/list")
    public Result<List<WfFormSchema>> listFormSchemas(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String schemaKey) {
        List<WfFormSchema> schemas = formSchemaService.listFormSchemas(status, schemaKey);
        return Result.success(schemas);
    }

    @GetMapping("/schema/{id}")
    public Result<WfFormSchema> getFormSchemaById(@PathVariable String id) {
        WfFormSchema schema = formSchemaService.getFormSchemaById(id);
        if (schema == null) return Result.error("表单不存在: " + id);
        return Result.success(schema);
    }

    @GetMapping("/schema/by-key/{key}")
    public Result<WfFormSchema> getFormSchemaByKey(@PathVariable String key) {
        WfFormSchema schema = formSchemaService.getFormSchemaByKey(key);
        if (schema == null) return Result.error("表单不存在: " + key);
        return Result.success(schema);
    }

    @PostMapping("/schema/validate")
    public Result<List<String>> validateFormData(@RequestBody Map<String, String> body) {
        String jsonSchema = body.get("jsonSchema");
        String businessData = body.get("businessData");
        List<String> errors = formSchemaService.validateFormData(jsonSchema, businessData);
        if (errors.isEmpty()) return Result.success(Collections.singletonList("验证通过"));
        return Result.error(400, String.join("; ", errors));
    }

    @GetMapping("/schema/{id}/fields")
    public Result<List<Map<String, Object>>> getFormFields(@PathVariable String id) {
        WfFormSchema schema = formSchemaService.getFormSchemaById(id);
        if (schema == null) return Result.error("表单不存在: " + id);
        return Result.success(formSchemaService.extractFields(schema.getJsonSchema()));
    }
}
