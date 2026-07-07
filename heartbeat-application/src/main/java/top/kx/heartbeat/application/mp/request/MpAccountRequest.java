// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class MpAccountRequest {

    // 注释：声明当前成员或方法。
    private String id;
    // 注释：声明当前成员或方法。
    private String name;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("app_id")
    // 注释：声明当前成员或方法。
    private String appId;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("app_secret")
    // 注释：声明当前成员或方法。
    private String appSecret;
    // 注释：声明当前成员或方法。
    private String token;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("aes_key")
    // 注释：声明当前成员或方法。
    private String aesKey;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
