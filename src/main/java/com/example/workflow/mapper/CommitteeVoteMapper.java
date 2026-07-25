package com.example.workflow.mapper;

import com.example.workflow.entity.WfCommitteeVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommitteeVoteMapper {

    int insert(WfCommitteeVote vote);

    int updateVote(@Param("subTaskId") String subTaskId,
                   @Param("vote") String vote,
                   @Param("comment") String comment);

    List<WfCommitteeVote> selectByTaskId(@Param("taskId") String taskId);

    List<WfCommitteeVote> selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    /** 统计某委员会的投票结果 */
    int countByTaskAndVote(@Param("taskId") String taskId, @Param("vote") String vote);
}
