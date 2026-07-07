// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformSocialProviderRequest {

    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("provider")
    // 注释：声明当前成员或方法。
    private String providerCode;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("name")
    // 注释：声明当前成员或方法。
    private String providerName;
    // 注释：声明当前成员或方法。
    private String providerType;
    // 注释：声明当前成员或方法。
    private String clientId;
    // 注释：声明当前成员或方法。
    private String appKey;
    // 注释：声明当前成员或方法。
    private String appSecretCipher;
    // 注释：声明当前成员或方法。
    private String authorizeUrl;
    // 注释：声明当前成员或方法。
    private String tokenUrl;
    // 注释：声明当前成员或方法。
    private String userInfoUrl;
    // 注释：声明当前成员或方法。
    private String scopes;
    // 注释：声明当前成员或方法。
    private Boolean enabled;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
