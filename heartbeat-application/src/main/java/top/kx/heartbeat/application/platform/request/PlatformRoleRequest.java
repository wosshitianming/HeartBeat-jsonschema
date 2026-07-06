package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
