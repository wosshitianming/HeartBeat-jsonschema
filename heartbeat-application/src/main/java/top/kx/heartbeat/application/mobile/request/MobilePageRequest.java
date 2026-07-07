package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载移动端配置请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class MobilePageRequest {

    @JsonAlias("app_id")
    private String appId;
    private String name;
    @JsonAlias("page_key")
    private String pageKey;
    @JsonAlias("route_path")
    private String routePath;
    private Object schema;
    private Integer sortNo;
    private String status;
}
