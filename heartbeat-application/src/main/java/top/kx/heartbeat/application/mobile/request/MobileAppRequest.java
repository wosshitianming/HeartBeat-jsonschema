package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MobileAppRequest {

    private String name;
    @JsonAlias("app_key")
    private String appKey;
    @JsonAlias("entry_url")
    private String entryUrl;
    private String status;
    private Object config;
}
