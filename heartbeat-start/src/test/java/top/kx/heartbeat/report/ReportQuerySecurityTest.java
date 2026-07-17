package top.kx.heartbeat.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.ReportService;
import top.kx.heartbeat.application.report.port.ReportDatasetRepository;
import top.kx.heartbeat.application.report.port.ReportQueryRepository;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportQuerySecurityTest {

    private ReportService service;
    private ReportDatasetRepository datasetRepository;
    private ReportQueryRepository queryRepository;

    @BeforeEach
    void setUp() {
        service = new ReportService();
        datasetRepository = mock(ReportDatasetRepository.class);
        queryRepository = mock(ReportQueryRepository.class);
        ReflectionTestUtils.setField(service, "datasetRepository", datasetRepository);
        ReflectionTestUtils.setField(service, "queryRepository", queryRepository);
        ReflectionTestUtils.setField(service, "dynamicSqlEnabled", true);
    }

    @Test
    void datasetSqlMustBeSingleReadonlySelect() {
        ReportDatasetRequest request = new ReportDatasetRequest();
        request.setQuerySql("select * from sys_user; delete from sys_user");

        assertThrows(IllegalArgumentException.class, () -> service.saveDataset(request));
        verify(datasetRepository, never()).saveDataset(any(ReportDatasetRequest.class));
    }

    @Test
    void queryLimitIsCappedBeforeReachingPersistence() {
        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("querySql", "select id, amount from pay_order where tenant_id = #{params.tenantId}");
        when(datasetRepository.getDataset("dataset-1")).thenReturn(DomainRecord.of(dataset));
        when(queryRepository.query(anyString(), anyMap(), anyInt()))
                .thenReturn(Collections.emptyList());

        service.query("dataset-1", Collections.emptyMap(), 100_000);

        verify(queryRepository).query(
                "select id, amount from pay_order where tenant_id = #{params.tenantId}",
                Collections.emptyMap(),
                5_000);
    }

    @Test
    void dynamicSqlIsFailClosedWhenTheFeatureFlagIsDisabled() {
        ReflectionTestUtils.setField(service, "dynamicSqlEnabled", false);

        assertThrows(IllegalStateException.class,
                () -> service.query("dataset-1", Collections.emptyMap(), 100));

        verify(datasetRepository, never()).getDataset(anyString());
        verify(queryRepository, never()).query(anyString(), anyMap(), anyInt());
    }
}
