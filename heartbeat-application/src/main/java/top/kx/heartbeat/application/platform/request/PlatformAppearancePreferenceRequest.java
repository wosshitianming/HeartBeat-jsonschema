package top.kx.heartbeat.application.platform.request;

import lombok.Data;

@Data
public class PlatformAppearancePreferenceRequest {

    private String colorMode;
    private Boolean fluidEnabled;
    private String accentColor;
    private String visualStyle;
}
