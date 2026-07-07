package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PlatformSocialProviderRequest {

    @JsonAlias("provider")
    private String providerCode;
    @JsonAlias("name")
    private String providerName;
    private String providerType;
    private String clientId;
    private String appKey;
    private String appSecretCipher;
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String scopes;
    private Boolean enabled;
    private String status;
}
