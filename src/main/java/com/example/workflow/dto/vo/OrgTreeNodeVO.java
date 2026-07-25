package com.example.workflow.dto.vo;

import lombok.Data;
import java.util.List;

@Data
public class OrgTreeNodeVO {
    private String id;
    private String orgName;
    private String parentId;
    private Integer orgLevel;
    private String orgType;
    private String orgCode;
    private Integer sortOrder;
    private Integer status;
    private List<OrgTreeNodeVO> children;
}
