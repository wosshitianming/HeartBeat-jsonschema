package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
