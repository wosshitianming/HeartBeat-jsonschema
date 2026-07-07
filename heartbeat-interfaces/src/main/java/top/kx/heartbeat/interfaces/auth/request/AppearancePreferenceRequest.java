// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;
import top.kx.heartbeat.application.platform.request.PlatformAppearancePreferenceRequest;


/**
 * 外观偏好保存请求对象。
 *
 * <p>用于承接当前用户主题、视觉风格等偏好配置。</p>
 */

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class AppearancePreferenceRequest {

    /**
     * 颜色模式。
     */
    // 注释：声明当前成员或方法。
    private String colorMode;

    /**
     * 是否启用流体布局。
     */
    // 注释：声明当前成员或方法。
    private Boolean fluidEnabled;

    /**
     * 主题强调色。
     */
    // 注释：声明当前成员或方法。
    private String accentColor;

    /**
     * 视觉风格。
     */
    // 注释：声明当前成员或方法。
    private String visualStyle;

    /**
     * 转换为平台配置服务需要的字段映射。
     *
     * @return 外观偏好字段映射
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public PlatformAppearancePreferenceRequest toPlatformRequest() {
        // 注释：设置或计算当前变量值。
        PlatformAppearancePreferenceRequest request = new PlatformAppearancePreferenceRequest();
        // 注释：执行当前代码行。
        request.setColorMode(colorMode);
        // 注释：执行当前代码行。
        request.setFluidEnabled(fluidEnabled);
        // 注释：执行当前代码行。
        request.setAccentColor(accentColor);
        // 注释：执行当前代码行。
        request.setVisualStyle(visualStyle);
        // 注释：返回当前处理结果。
        return request;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
