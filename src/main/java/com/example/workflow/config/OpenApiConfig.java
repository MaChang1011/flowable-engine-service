package com.example.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flowable 多机构工作流引擎 API")
                        .description("""
                                基于 Flowable 6.8 的多机构、多部门工作流审批引擎。
                                
                                ## 核心功能
                                - **流程定义管理** — 部署、查看、启停 BPMN 流程
                                - **流程实例管理** — 启动、终止、挂起、激活实例
                                - **任务管理** — 待办、已办、完成、驳回、委派
                                - **表单设计器** — JSON Schema 表单创建与验证
                                - **组织架构** — 递归机构树、角色权限
                                
                                ## 权限模型
                                通过 Header 传递用户上下文：
                                - `X-User-Id` — 当前用户 ID
                                - `X-Org-Id` — 当前机构 ID
                                - `X-Role-Ids` — 角色 ID 列表
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Hermes Farm")
                                .email("admin@hermes-farm.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://hermes-farm.com")));
    }
}
