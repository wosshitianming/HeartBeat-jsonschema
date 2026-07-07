package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;
import top.kx.heartbeat.application.platform.request.PlatformAppearancePreferenceRequest;


/**
 * 承载认证登录请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class AppearancePreferenceRequest {

    private String colorMode;

    private Boolean fluidEnabled;

    private String accentColor;

    private String visualStyle;

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异。
     *
     * @return 处理后的业务结果。
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
