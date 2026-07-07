// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.mobile.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class MobileApiRouteRequest {

    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("app_id")
    // 注释：声明当前成员或方法。
    private String appId;
    // 注释：声明当前成员或方法。
    private String name;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("route_key")
    // 注释：声明当前成员或方法。
    private String routeKey;
    // 注释：声明当前成员或方法。
    private String method;
    // 注释：声明当前成员或方法。
    private String path;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("target_url")
    // 注释：声明当前成员或方法。
    private String targetUrl;
    // 注释：声明当前成员或方法。
    private Integer sortNo;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
