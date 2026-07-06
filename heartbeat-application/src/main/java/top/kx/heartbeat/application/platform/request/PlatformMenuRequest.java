package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PlatformMenuRequest {

    private String parentId;
    private String menuCode;
    @JsonAlias("name")
    private String menuName;
    @JsonAlias("type")
    private String menuType;
    @JsonAlias("path")
    private String routePath;
    @JsonAlias("component")
    private String componentPath;
    private String redirectPath;
    private String icon;
    private Boolean visible;
    private Boolean keepAlive;
    private String externalLink;
    @JsonAlias("permission")
    private String permissionMode;
    private Integer sortNo;
    private String status;
}
