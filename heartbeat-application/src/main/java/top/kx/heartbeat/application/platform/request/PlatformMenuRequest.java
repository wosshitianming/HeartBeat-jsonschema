// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformMenuRequest {

    // 注释：声明当前成员或方法。
    private String parentId;
    // 注释：声明当前成员或方法。
    private String menuCode;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("name")
    // 注释：声明当前成员或方法。
    private String menuName;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("type")
    // 注释：声明当前成员或方法。
    private String menuType;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("path")
    // 注释：声明当前成员或方法。
    private String routePath;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("component")
    // 注释：声明当前成员或方法。
    private String componentPath;
    // 注释：声明当前成员或方法。
    private String redirectPath;
    // 注释：声明当前成员或方法。
    private String icon;
    // 注释：声明当前成员或方法。
    private Boolean visible;
    // 注释：声明当前成员或方法。
    private Boolean keepAlive;
    // 注释：声明当前成员或方法。
    private String externalLink;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("permission")
    // 注释：声明当前成员或方法。
    private String permissionMode;
    // 注释：声明当前成员或方法。
    private Integer sortNo;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
