package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
