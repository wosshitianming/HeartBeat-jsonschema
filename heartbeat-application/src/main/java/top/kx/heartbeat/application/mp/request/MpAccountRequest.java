package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
