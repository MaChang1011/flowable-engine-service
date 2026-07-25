package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WfCommitteeVote {
    private String id;
    private String processInstanceId;
    private String taskId;
    private String subTaskId;
    private String committeeName;
    private String committeeMemberId;
    private String vote;       // APPROVE / REJECT / ABSTAIN
    private String comment;
    private LocalDateTime votedAt;
}
