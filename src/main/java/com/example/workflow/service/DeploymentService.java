package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final RepositoryService repositoryService;

    /**
     * 部署流程定义（支持 .bpmn / .bpmn20.xml / .zip）
     */
    public String deploy(MultipartFile file, String tenantId) throws IOException {
        String deploymentName = "deployment_" + UUID.randomUUID().toString().substring(0, 8);
        
        var builder = repositoryService.createDeployment()
                .name(deploymentName)
                .addInputStream(file.getOriginalFilename(), file.getInputStream());
        
        if (StringUtils.hasText(tenantId)) {
            builder.tenantId(tenantId);
        }
        
        return builder.deploy().getId();
    }
}
