package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载公众号管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class MpAccountRequest {

    private String id;
    private String name;
    @JsonAlias("app_id")
    private String appId;
    @JsonAlias("app_secret")
    private String appSecret;
    private String token;
    @JsonAlias("aes_key")
    private String aesKey;
    private String status;
}
