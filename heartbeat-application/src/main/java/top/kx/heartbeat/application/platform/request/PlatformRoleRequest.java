// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformRoleRequest {

    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("code")
    // 注释：声明当前成员或方法。
    private String roleCode;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("name")
    // 注释：声明当前成员或方法。
    private String roleName;
    // 注释：声明当前成员或方法。
    private String roleType;
    // 注释：声明当前成员或方法。
    private String dataScope;
    // 注释：声明当前成员或方法。
    private String description;
    // 注释：声明当前成员或方法。
    private Integer sortNo;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
