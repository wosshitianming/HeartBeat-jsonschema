package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PlatformRoleRequest {

    @JsonAlias("code")
    private String roleCode;
    @JsonAlias("name")
    private String roleName;
    private String roleType;
    private String dataScope;
    private String description;
    private Integer sortNo;
    private String status;
}
