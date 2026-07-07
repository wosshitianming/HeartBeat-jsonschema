// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformConfigurationRequest {

    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("key")
    // 注释：声明当前成员或方法。
    private String configKey;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("name")
    // 注释：声明当前成员或方法。
    private String configName;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("value")
    // 注释：声明当前成员或方法。
    private String configValue;
    // 注释：声明当前成员或方法。
    private String valueType;
    // 注释：声明当前成员或方法。
    private Boolean encrypted;
    // 注释：声明当前成员或方法。
    private String configGroup;
    // 注释：声明当前成员或方法。
    private String description;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
