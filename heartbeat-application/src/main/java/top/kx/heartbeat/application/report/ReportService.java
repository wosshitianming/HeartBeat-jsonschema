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

    public List<RecordResponse> listDatasets() {
        return RecordResponse.fromMaps(maps(datasetRepository.listDatasets()));
    }

    @Transactional
    public RecordResponse saveDataset(ReportDatasetRequest request) {
        assertReadonlySql(request.getQuerySql());
        return RecordResponse.from(datasetRepository.saveDataset(request));
    }

    public List<RecordResponse> listTemplates() {
        return RecordResponse.fromMaps(maps(templateRepository.listTemplates()));
    }

    @Transactional
    public RecordResponse saveTemplate(ReportTemplateRequest request) {
        return RecordResponse.from(templateRepository.saveTemplate(request));
    }

    public List<RecordResponse> query(String datasetId, Map<String, Object> params, Integer limit) {
        return RecordResponse.fromMaps(queryRows(datasetId, params, limit));
    }

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

    private List<Map<String, Object>> queryRows(String datasetId, Map<String, Object> params, Integer limit) {
        Map<String, Object> dataset = datasetRepository.getDataset(datasetId).toMap();
        String sql = stringValue(dataset.get("querySql"));
        assertReadonlySql(sql);
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
        return maps(queryRepository.query(sql, safeParams, normalizeLimit(limit)));
    }

    private void assertReadonlySql(String sql) {
        String normalized = sql == null ? "" : sql.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.startsWith(READONLY_SQL_PREFIX) || normalized.contains(";")) {
            throw new IllegalArgumentException("Report dataset only supports single SELECT SQL");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String joinCsv(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }
}
