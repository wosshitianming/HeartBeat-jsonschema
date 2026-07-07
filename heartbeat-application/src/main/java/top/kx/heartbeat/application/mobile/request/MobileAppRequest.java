package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载移动端配置请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
