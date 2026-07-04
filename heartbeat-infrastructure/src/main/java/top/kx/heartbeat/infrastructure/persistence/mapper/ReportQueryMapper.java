package top.kx.heartbeat.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportQueryMapper {

    List<Map<String, Object>> executeReportQuery(@Param("sql") String sql, @Param("params") Map<String, Object> params, @Param("limit") int limit);
}
