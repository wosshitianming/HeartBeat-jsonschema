package top.kx.heartbeat.application.platform.request;

import lombok.Data;

@Data
public class PlatformSocialBindRequest {

    private String userId;
    private String provider;
    private String openId;
    private String unionId;
    private String nickname;
    private String avatar;
}
