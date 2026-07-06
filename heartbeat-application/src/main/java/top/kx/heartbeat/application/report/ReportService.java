package top.kx.heartbeat.application.report;


import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.report.port.ReportRepository;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 报表应用服务。
 *
 * <p>负责报表数据集、报表模板、数据查询和 CSV 导出编排。</p>
 */
@Service
public class ReportService {

    /**
     * 默认查询行数上限。
     */
    private static final int DEFAULT_LIMIT = 500;

    /**
     * 最大查询行数上限。
     */
    private static final int MAX_LIMIT = 5000;

    /**
     * 只读查询 SQL 前缀。
     */
    private static final String READONLY_SQL_PREFIX = "select";

    /**
     * 报表仓储。
     */
    @Resource
    private ReportRepository reportRepository;

    /**
     * 查询报表数据集列表。
     *
     * @return 报表数据集列表。
     */
    public List<RecordResponse> listDatasets() {
        // 查询报表数据集领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(reportRepository.listDatasets()));
    }

    /**
     * 保存报表数据集。
     *
     * @param command 报表数据集保存命令。
     * @return 保存后的报表数据集。
     */
    @Transactional
    public RecordResponse saveDataset(Map<String, Object> command) {
        // 校验数据集 SQL 只能是只读查询。
        assertReadonlySql(stringValue(command.get("querySql")));
        // 委托仓储保存报表数据集并返回字段 Map。
        return RecordResponse.from(reportRepository.saveDataset(command));
    }

    /**
     * 查询报表模板列表。
     *
     * @return 报表模板列表。
     */
    public List<RecordResponse> listTemplates() {
        // 查询报表模板领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(reportRepository.listTemplates()));
    }

    /**
     * 保存报表模板。
     *
     * @param command 报表模板保存命令。
     * @return 保存后的报表模板。
     */
    @Transactional
    public RecordResponse saveTemplate(Map<String, Object> command) {
        // 委托仓储保存报表模板并返回字段 Map。
        return RecordResponse.from(reportRepository.saveTemplate(command));
    }

    /**
     * 查询报表数据。
     *
     * @param datasetId 数据集标识。
     * @param params 查询参数。
     * @param limit 查询行数上限。
     * @return 报表数据行。
     */
    public List<RecordResponse> query(String datasetId, Map<String, Object> params, Integer limit) {
        // 复用内部查询方法执行数据集查询。
        return RecordResponse.fromMaps(queryRows(datasetId, params, limit));
    }

    /**
     * 执行报表数据集查询。
     *
     * @param datasetId 数据集标识。
     * @param params 查询参数。
     * @param limit 查询行数上限。
     * @return 报表数据行。
     */
    private List<Map<String, Object>> queryRows(String datasetId, Map<String, Object> params, Integer limit) {
        // 查询数据集定义。
        Map<String, Object> dataset = reportRepository.getDataset(datasetId).toMap();
        // 读取数据集查询 SQL。
        String sql = stringValue(dataset.get("querySql"));
        // 查询前再次校验 SQL 只读性。
        assertReadonlySql(sql);
        // 委托仓储执行查询并转换为字段 Map 列表。
        return maps(reportRepository.query(sql, params == null ? Collections.emptyMap() : params, normalizeLimit(limit)));
    }

    /**
     * 导出报表 CSV。
     *
     * @param datasetId 数据集标识。
     * @param params 查询参数。
     * @param limit 查询行数上限。
     * @return CSV 字节数组。
     */
    public byte[] exportCsv(String datasetId, Map<String, Object> params, Integer limit) {
        // 查询待导出的报表数据行。
        List<Map<String, Object>> rows = queryRows(datasetId, params, limit);
        // 创建 CSV 文本构造器。
        StringBuilder csv = new StringBuilder();
        // 存在数据行时写入表头和数据。
        if (CollectionUtils.isNotEmpty(rows)) {
            // 使用首行字段顺序作为 CSV 表头。
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            // 写入 CSV 表头行。
            csv.append(joinCsv(headers)).append('\n');
            // 遍历报表数据行。
            for (Map<String, Object> row : rows) {
                // 创建当前行 CSV 字段值。
                List<String> values = new ArrayList<>();
                // 按表头顺序读取字段值。
                for (String header : headers) {
                    // 空值输出为空字符串，非空值输出字符串形式。
                    values.add(row.get(header) == null ? "" : String.valueOf(row.get(header)));
                }
                // 写入当前 CSV 数据行。
                csv.append(joinCsv(values)).append('\n');
            }
        }
        // 使用 UTF-8 输出 CSV 字节。
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验 SQL 是否为只读查询。
     *
     * @param sql SQL 文本。
     */
    private void assertReadonlySql(String sql) {
        // 统一转换为小写 SQL 便于安全校验。
        String normalized = sql == null ? "" : sql.trim().toLowerCase(java.util.Locale.ROOT);
        // 仅允许单条 SELECT 查询，禁止分号拼接多语句。
        if (!normalized.startsWith(READONLY_SQL_PREFIX) || normalized.contains(";")) {
            // 抛出只读 SQL 校验异常。
            throw new IllegalArgumentException("报表数据集仅允许单条 SELECT 查询");
        }
    }

    /**
     * 规范化查询行数上限。
     *
     * @param limit 原始查询行数上限。
     * @return 规范化后的查询行数上限。
     */
    private int normalizeLimit(Integer limit) {
        // 未传或非正数时使用默认上限。
        if (limit == null || limit <= 0) {
            // 返回默认查询行数上限。
            return DEFAULT_LIMIT;
        }
        // 用户上限不能超过系统最大上限。
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 拼接 CSV 行。
     *
     * @param values 字段值列表。
     * @return CSV 行文本。
     */
    private String joinCsv(List<String> values) {
        // 逐个字段加引号并转义内部引号。
        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(java.util.stream.Collectors.joining(","));
    }

    /**
     * 将对象转换为去空白字符串。
     *
     * @param value 原始值。
     * @return 去空白字符串。
     */
    private String stringValue(Object value) {
        // 空值转换为空字符串，非空值转换为字符串并去除两侧空白。
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将领域记录列表转换为字段 Map 列表。
     *
     * @param records 领域记录列表。
     * @return 字段 Map 列表。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 逐条导出领域记录的字段副本。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
