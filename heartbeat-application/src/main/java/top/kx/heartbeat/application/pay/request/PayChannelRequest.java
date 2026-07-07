package top.kx.heartbeat.application.pay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PayChannelRequest {

    private String name;
    private String provider;
    @JsonAlias("app_id")
    private String appId;
    @JsonAlias("app_secret")
    private String appSecret;
    private String status;
    private Integer sortNo;
    private Object config;
}
