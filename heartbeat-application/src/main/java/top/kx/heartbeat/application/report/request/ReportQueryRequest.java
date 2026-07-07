package top.kx.heartbeat.application.report.request;

import lombok.Data;

/**
 * 承载报表管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class ReportQueryRequest {

    private Object params;
}
