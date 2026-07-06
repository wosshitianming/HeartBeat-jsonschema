package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;
import top.kx.heartbeat.application.platform.request.PlatformAppearancePreferenceRequest;


/**
 * 外观偏好保存请求对象。
 *
 * <p>用于承接当前用户主题、视觉风格等偏好配置。</p>
 */
@Data
public class AppearancePreferenceRequest {

    /**
     * 颜色模式。
     */
    private String colorMode;

    /**
     * 是否启用流体布局。
     */
    private Boolean fluidEnabled;

    /**
     * 主题强调色。
     */
    private String accentColor;

    /**
     * 视觉风格。
     */
    private String visualStyle;

    /**
     * 转换为平台配置服务需要的字段映射。
     *
     * @return 外观偏好字段映射
     */
    public PlatformAppearancePreferenceRequest toPlatformRequest() {
        PlatformAppearancePreferenceRequest request = new PlatformAppearancePreferenceRequest();
        request.setColorMode(colorMode);
        request.setFluidEnabled(fluidEnabled);
        request.setAccentColor(accentColor);
        request.setVisualStyle(visualStyle);
        return request;
    }
}
