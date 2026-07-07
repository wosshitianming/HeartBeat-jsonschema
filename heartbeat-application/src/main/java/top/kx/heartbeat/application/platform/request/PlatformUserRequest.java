// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Date;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PlatformUserRequest {

    // 注释：声明当前成员或方法。
    private String deptId;
    // 注释：声明当前成员或方法。
    private String username;
    // 注释：声明当前成员或方法。
    private String nickname;
    // 注释：声明当前成员或方法。
    private String realName;
    // 注释：声明当前成员或方法。
    private String email;
    // 注释：声明当前成员或方法。
    private String phone;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("avatar")
    // 注释：声明当前成员或方法。
    private String avatarUrl;
    // 注释：声明当前成员或方法。
    private String password;
    // 注释：声明当前成员或方法。
    private String passwordHash;
    // 注释：声明当前成员或方法。
    private String passwordAlgo;
    // 注释：声明当前成员或方法。
    private Date passwordUpdateTime;
    // 注释：声明当前成员或方法。
    private String gender;
    // 注释：声明当前成员或方法。
    private String userType;
    // 注释：声明当前成员或方法。
    private String status;
// 注释：结束当前代码块。
}
