package top.kx.heartbeat.application.report;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.report.port.ReportDatasetRepository;
import top.kx.heartbeat.application.report.port.ReportQueryRepository;
import top.kx.heartbeat.application.report.port.ReportTemplateRepository;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;
import top.kx.heartbeat.application.report.request.ReportTemplateRequest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 编排报表管理应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class ReportService {

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final String READONLY_SQL_PREFIX = "select";

    @Resource
    private ReportDatasetRepository datasetRepository;

    @Resource
    private ReportTemplateRepository templateRepository;

    @Resource
    private ReportQueryRepository queryRepository;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调报表管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDatasets() {
        return RecordResponse.fromMaps(maps(datasetRepository.listDatasets()));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调报表管理相关仓储和领域规则。
     *
     * @param request 报表管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveDataset(ReportDatasetRequest request) {
        assertReadonlySql(request.getQuerySql());
        return RecordResponse.from(datasetRepository.saveDataset(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调报表管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listTemplates() {
        return RecordResponse.fromMaps(maps(templateRepository.listTemplates()));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调报表管理相关仓储和领域规则。
     *
     * @param request 报表管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveTemplate(ReportTemplateRequest request) {
        return RecordResponse.from(templateRepository.saveTemplate(request));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调报表管理相关仓储和领域规则。
     *
     * @param datasetId 业务记录标识。
     * @param params 业务处理所需参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> query(String datasetId, Map<String, Object> params, Integer limit) {
        return RecordResponse.fromMaps(queryRows(datasetId, params, limit));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调报表管理相关仓储和领域规则。
     *
     * @param datasetId 业务记录标识。
     * @param params 业务处理所需参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    public byte[] exportCsv(String datasetId, Map<String, Object> params, Integer limit) {
        List<Map<String, Object>> rows = queryRows(datasetId, params, limit);
        StringBuilder csv = new StringBuilder();
        if (CollectionUtils.isNotEmpty(rows)) {
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            csv.append(joinCsv(headers)).append('\n');
            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(row.get(header) == null ? "" : String.valueOf(row.get(header)));
                }
                csv.append(joinCsv(values)).append('\n');
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调报表管理相关仓储和领域规则。
     *
     * @param datasetId 业务记录标识。
     * @param params 业务处理所需参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> queryRows(String datasetId, Map<String, Object> params, Integer limit) {
        Map<String, Object> dataset = datasetRepository.getDataset(datasetId).toMap();
        String sql = stringValue(dataset.get("querySql"));
        assertReadonlySql(sql);
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
        return maps(queryRepository.query(sql, safeParams, normalizeLimit(limit)));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调报表管理相关仓储和领域规则。
     *
     * @param sql 业务处理所需参数。
     */
    private void assertReadonlySql(String sql) {
        String normalized = sql == null ? "" : sql.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.startsWith(READONLY_SQL_PREFIX) || normalized.contains(";")) {
            throw new IllegalArgumentException("Report dataset only supports single SELECT SQL");
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调报表管理相关仓储和领域规则。
     *
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调报表管理相关仓储和领域规则。
     *
     * @param values 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String joinCsv(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调报表管理相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调报表管理相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }
}
