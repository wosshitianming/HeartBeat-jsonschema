package top.kx.heartbeat.application.platform.request;

import lombok.Data;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PlatformSocialBindRequest {

    private String userId;
    private String provider;
    private String openId;
    private String unionId;
    private String nickname;
    private String avatar;
}
