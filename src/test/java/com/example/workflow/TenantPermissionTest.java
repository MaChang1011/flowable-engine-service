package com.example.workflow;

import com.example.workflow.service.OrgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TenantPermissionTest {

    @Autowired
    private OrgService orgService;

    @Test
    void testRecursiveTenantQuery() {
        // 总公司 → 能看到所有租户
        List<String> all = orgService.getAccessibleTenantIds("ORG_TENANT_001");
        assertEquals(5, all.size());
        assertTrue(all.contains("ORG_TENANT_001"));
        assertTrue(all.contains("ORG_TENANT_003"));
        assertTrue(all.contains("ORG_TENANT_005"));

        // 华东分公司 → 只能看自己和下属
        List<String> eastChina = orgService.getAccessibleTenantIds("ORG_TENANT_002");
        assertEquals(3, eastChina.size());
        assertTrue(eastChina.contains("ORG_TENANT_002"));
        assertTrue(eastChina.contains("ORG_TENANT_003"));
        assertFalse(eastChina.contains("ORG_TENANT_004"));

        // 上海办事处 → 只看自己
        List<String> shanghai = orgService.getAccessibleTenantIds("ORG_TENANT_003");
        assertEquals(1, shanghai.size());
        assertEquals("ORG_TENANT_003", shanghai.get(0));
    }

    @Test
    void testEmptyTenantId() {
        List<String> result = orgService.getAccessibleTenantIds("");
        assertTrue(result.isEmpty());
        
        result = orgService.getAccessibleTenantIds(null);
        assertTrue(result.isEmpty());
    }
}
