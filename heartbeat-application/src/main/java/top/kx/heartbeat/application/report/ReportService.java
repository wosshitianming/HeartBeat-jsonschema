// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class ReportService {

    // 注释：声明当前成员或方法。
    private static final int DEFAULT_LIMIT = 500;
    // 注释：声明当前成员或方法。
    private static final int MAX_LIMIT = 5000;
    // 注释：声明当前成员或方法。
    private static final String READONLY_SQL_PREFIX = "select";

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportDatasetRepository datasetRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportTemplateRepository templateRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportQueryRepository queryRepository;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDatasets() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(datasetRepository.listDatasets()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveDataset(ReportDatasetRequest request) {
        // 注释：执行当前代码行。
        assertReadonlySql(request.getQuerySql());
        // 注释：返回当前处理结果。
        return RecordResponse.from(datasetRepository.saveDataset(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listTemplates() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(templateRepository.listTemplates()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveTemplate(ReportTemplateRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(templateRepository.saveTemplate(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> query(String datasetId, Map<String, Object> params, Integer limit) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(queryRows(datasetId, params, limit));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public byte[] exportCsv(String datasetId, Map<String, Object> params, Integer limit) {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> rows = queryRows(datasetId, params, limit);
        // 注释：设置或计算当前变量值。
        StringBuilder csv = new StringBuilder();
        // 注释：判断当前业务条件。
        if (CollectionUtils.isNotEmpty(rows)) {
            // 注释：设置或计算当前变量值。
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            // 注释：执行当前代码行。
            csv.append(joinCsv(headers)).append('\n');
            // 注释：遍历当前数据集合。
            for (Map<String, Object> row : rows) {
                // 注释：设置或计算当前变量值。
                List<String> values = new ArrayList<>();
                // 注释：遍历当前数据集合。
                for (String header : headers) {
                    // 注释：设置或计算当前变量值。
                    values.add(row.get(header) == null ? "" : String.valueOf(row.get(header)));
                    // 注释：结束当前代码块。
                }
                // 注释：执行当前代码行。
                csv.append(joinCsv(values)).append('\n');
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return csv.toString().getBytes(StandardCharsets.UTF_8);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> queryRows(String datasetId, Map<String, Object> params, Integer limit) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> dataset = datasetRepository.getDataset(datasetId).toMap();
        // 注释：设置或计算当前变量值。
        String sql = stringValue(dataset.get("querySql"));
        // 注释：执行当前代码行。
        assertReadonlySql(sql);
        // 注释：设置或计算当前变量值。
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
        // 注释：返回当前处理结果。
        return maps(queryRepository.query(sql, safeParams, normalizeLimit(limit)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void assertReadonlySql(String sql) {
        // 注释：设置或计算当前变量值。
        String normalized = sql == null ? "" : sql.trim().toLowerCase(java.util.Locale.ROOT);
        // 注释：判断当前业务条件。
        if (!normalized.startsWith(READONLY_SQL_PREFIX) || normalized.contains(";")) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Report dataset only supports single SELECT SQL");
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private int normalizeLimit(Integer limit) {
        // 注释：判断当前业务条件。
        if (limit == null || limit <= 0) {
            // 注释：返回当前处理结果。
            return DEFAULT_LIMIT;
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return Math.min(limit, MAX_LIMIT);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String joinCsv(List<String> values) {
        // 注释：返回当前处理结果。
        return values.stream()
                // 注释：继续当前链式调用。
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                // 注释：继续当前链式调用。
                .collect(Collectors.joining(","));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
