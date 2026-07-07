package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

public interface ReportQueryRepository {

    List<DomainRecord> query(String sql, Map<String, Object> params, int limit);
}
