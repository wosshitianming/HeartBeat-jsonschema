// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;
import top.kx.heartbeat.application.platform.request.PlatformLoginRequest;


/**
 * 账号密码登录请求对象。
 *
 * <p>用于承接后台登录时提交的账号与密码。</p>
 */

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class LoginRequest {

    /**
     * 登录账号。
     */
    // 注释：声明当前成员或方法。
    private String username;

    /**
     * 登录密码。
     */
    // 注释：声明当前成员或方法。
    private String password;

    /**
     * 转换为平台认证服务需要的字段映射。
     *
     * @return 登录字段映射
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public PlatformLoginRequest toPlatformRequest() {
        // 注释：设置或计算当前变量值。
        PlatformLoginRequest request = new PlatformLoginRequest();
        // 注释：执行当前代码行。
        request.setUsername(username);
        // 注释：执行当前代码行。
        request.setPassword(password);
        // 注释：返回当前处理结果。
        return request;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
