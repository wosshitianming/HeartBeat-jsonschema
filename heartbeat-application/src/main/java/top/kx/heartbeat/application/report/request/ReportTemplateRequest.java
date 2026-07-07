package top.kx.heartbeat.application.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ReportTemplateRequest {

    private String id;
    @JsonAlias("dataset_id")
    private String datasetId;
    private String name;
    @JsonAlias("template_key")
    private String templateKey;
    private Object template;
    private String status;
}
