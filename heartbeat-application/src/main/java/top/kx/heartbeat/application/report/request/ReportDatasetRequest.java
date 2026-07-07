package top.kx.heartbeat.application.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载报表管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class ReportDatasetRequest {

    private String id;
    private String name;
    @JsonAlias("dataset_key")
    private String datasetKey;
    @JsonAlias("query_sql")
    private String querySql;
    private Object params;
    private String status;
}
