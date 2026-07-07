package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;

import java.util.List;

/**
 * 定义报表管理持久化端口，隔离应用层与具体数据访问实现。
 */
public interface ReportDatasetRepository {


    List<DomainRecord> listDatasets();


    DomainRecord saveDataset(ReportDatasetRequest request);


    DomainRecord getDataset(String id);
}
