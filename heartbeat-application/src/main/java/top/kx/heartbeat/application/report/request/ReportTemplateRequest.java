package top.kx.heartbeat.application.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载公众号管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
