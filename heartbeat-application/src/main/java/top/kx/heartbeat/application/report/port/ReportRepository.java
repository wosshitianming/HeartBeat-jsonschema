package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 报表/数据集用用网关接口
 *
 * @author heartbeat-team
 */
public interface ReportRepository {

    /**
     * 列出全部数据集
     */
    List<DomainRecord> listDatasets();

    /**
     * 保存数据集
     */
    DomainRecord saveDataset(Map<String, Object> command);

    /**
     * 列出全部报表模板
     */
    List<DomainRecord> listTemplates();

    /**
     * 保存模板
     */
    DomainRecord saveTemplate(Map<String, Object> command);

    /**
     * 查询单个数据集
     */
    DomainRecord getDataset(String id);

    /**
     * 执行 SQL 查询（受 limit 限制）
     */
    List<DomainRecord> query(String sql, Map<String, Object> params, int limit);
}
