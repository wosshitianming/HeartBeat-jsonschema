package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 定义报表管理持久化端口，隔离应用层与具体数据访问实现。
 */
public interface ReportQueryRepository {


    List<DomainRecord> query(String sql, Map<String, Object> params, int limit);
}
