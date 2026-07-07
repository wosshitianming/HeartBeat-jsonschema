package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
