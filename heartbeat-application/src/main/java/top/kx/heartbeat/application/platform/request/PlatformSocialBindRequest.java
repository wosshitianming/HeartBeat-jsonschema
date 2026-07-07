// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformSocialBindRequest {

    // 注释：声明当前成员或方法。
    private String userId;
    // 注释：声明当前成员或方法。
    private String provider;
    // 注释：声明当前成员或方法。
    private String openId;
    // 注释：声明当前成员或方法。
    private String unionId;
    // 注释：声明当前成员或方法。
    private String nickname;
    // 注释：声明当前成员或方法。
    private String avatar;
// 注释：结束当前代码块。
}
