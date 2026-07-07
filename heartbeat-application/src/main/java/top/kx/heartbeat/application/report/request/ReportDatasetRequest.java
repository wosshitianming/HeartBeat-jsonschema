package top.kx.heartbeat.application.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
