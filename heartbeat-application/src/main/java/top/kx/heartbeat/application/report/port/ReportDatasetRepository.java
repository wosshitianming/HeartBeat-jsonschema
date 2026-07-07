// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;

import java.util.List;

/**
 * 注释：当前接口用于声明对应业务能力。
 */
public interface ReportDatasetRepository {

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listDatasets();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord saveDataset(ReportDatasetRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord getDataset(String id);
// 注释：结束当前代码块。
}
