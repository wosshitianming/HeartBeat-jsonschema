package top.kx.heartbeat.infrastructure.persistence.mapper.flow;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface FlowRunRuntimeMapper {

    Map<String, Object> selectRunForUpdate(@Param("id") Long id, @Param("tenantId") long tenantId);

    Long selectRunIdByEngineInstanceId(@Param("tenantId") long tenantId,
                                       @Param("engineInstanceId") String engineInstanceId);

    Map<String, Object> summarize(@Param("tenantId") long tenantId,
                                  @Param("flowId") Long flowId,
                                  @Param("startedAfter") Date startedAfter,
                                  @Param("startedBefore") Date startedBefore);

    int updateRunRuntime(@Param("id") Long id,
                         @Param("tenantId") long tenantId,
                         @Param("values") Map<String, Object> values);

    Long selectLastEventSequenceForUpdate(@Param("id") Long id, @Param("tenantId") long tenantId);

    int updateLastEventSequence(@Param("id") Long id,
                                @Param("tenantId") long tenantId,
                                @Param("eventSeq") long eventSeq);

    int updateEventRuntime(@Param("id") Long id,
                           @Param("tenantId") long tenantId,
                           @Param("values") Map<String, Object> values);

    Map<String, Object> selectRunRuntime(@Param("id") Long id, @Param("tenantId") long tenantId);

    List<Map<String, Object>> selectRunRuntimes(@Param("tenantId") long tenantId,
                                                @Param("ids") List<Long> ids);

    List<Map<String, Object>> selectEventRuntimes(@Param("tenantId") long tenantId,
                                                  @Param("ids") List<Long> ids);
}
