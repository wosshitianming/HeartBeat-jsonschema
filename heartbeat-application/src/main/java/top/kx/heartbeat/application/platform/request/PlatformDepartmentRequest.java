// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformDepartmentRequest {

    // 注释：声明当前成员或方法。
    private String parentId;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("code")
    // 注释：声明当前成员或方法。
    private String deptCode;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("name")
    // 注释：声明当前成员或方法。
    private String deptName;
    // 注释：声明当前成员或方法。
    private String ancestors;
    // 注释：声明当前成员或方法。
    private Integer deptLevel;
    // 注释：声明当前成员或方法。
    private String leaderUserId;
    // 注释：声明当前成员或方法。
    private String phone;
    // 注释：声明当前成员或方法。
    private String email;
    // 注释：声明当前成员或方法。
    private Integer sortNo;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
