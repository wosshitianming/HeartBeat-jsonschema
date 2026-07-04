package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public Map<String, Object> toMap() {
        // 创建有序字段映射。
        Map<String, Object> payload = new LinkedHashMap<>();
        // 判断颜色模式是否需要更新。
        if (colorMode != null) {
            // 写入颜色模式字段。
            payload.put("colorMode", colorMode);
        }
        // 判断流体布局是否需要更新。
        if (fluidEnabled != null) {
            // 写入流体布局字段。
            payload.put("fluidEnabled", fluidEnabled);
        }
        // 判断主题强调色是否需要更新。
        if (accentColor != null) {
            // 写入主题强调色字段。
            payload.put("accentColor", accentColor);
        }
        // 判断视觉风格是否需要更新。
        if (visualStyle != null) {
            // 写入视觉风格字段。
            payload.put("visualStyle", visualStyle);
        }
        // 返回外观偏好字段映射。
        return payload;
    }
}
