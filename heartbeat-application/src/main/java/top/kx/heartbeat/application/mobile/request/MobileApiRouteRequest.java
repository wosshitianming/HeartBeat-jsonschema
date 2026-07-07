package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载移动端配置请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class MobileApiRouteRequest {

    @JsonAlias("app_id")
    private String appId;
    private String name;
    @JsonAlias("route_key")
    private String routeKey;
    private String method;
    private String path;
    @JsonAlias("target_url")
    private String targetUrl;
    private Integer sortNo;
    private String status;
}
