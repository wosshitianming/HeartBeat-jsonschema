package top.kx.heartbeat.report;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.application.report.ReportService;
import top.kx.heartbeat.domain.common.model.DomainRecord;
import top.kx.heartbeat.domain.report.ReportRepository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportQuerySecurityTest {

    @Test
    void datasetSqlMustBeSingleReadonlySelect() {
        ReportService service = new ReportService();
        ReflectionTestUtils.setField(service, "reportRepository", new FakeReportRepository());
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("querySql", "select * from sys_user; delete from sys_user");

        assertThrows(IllegalArgumentException.class, () -> service.saveDataset(command));
    }

    private static class FakeReportRepository implements ReportRepository {
        @Override public List<DomainRecord> listDatasets() { return Collections.emptyList(); }
        @Override public DomainRecord saveDataset(Map<String, Object> command) { return DomainRecord.of(command); }
        @Override public List<DomainRecord> listTemplates() { return Collections.emptyList(); }
        @Override public DomainRecord saveTemplate(Map<String, Object> command) { return DomainRecord.of(command); }
        @Override public DomainRecord getDataset(String id) { return DomainRecord.of(Collections.emptyMap()); }
        @Override public List<DomainRecord> query(String sql, Map<String, Object> params, int limit) { return Collections.emptyList(); }
    }
}
