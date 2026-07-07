package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;

import java.util.List;

public interface ReportDatasetRepository {

    List<DomainRecord> listDatasets();

    DomainRecord saveDataset(ReportDatasetRequest request);

    DomainRecord getDataset(String id);
}
