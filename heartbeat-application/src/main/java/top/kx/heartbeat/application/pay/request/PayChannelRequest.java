package top.kx.heartbeat.application.pay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载支付业务请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
