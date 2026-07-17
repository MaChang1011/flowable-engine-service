package com.example.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 需要 MySQL 才能运行这些集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
class ProcessDefinitionTest {

    @Autowired
    private RepositoryService repositoryService;

    @Test
    void testDeployAndList() {
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn20.xml")
                .tenantId("ORG_TENANT_001")
                .deploy()
                .getId();

        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId("ORG_TENANT_001")
                .list();

        assertTrue(defs.size() > 0, "应该至少有一个流程定义");
    }
}
