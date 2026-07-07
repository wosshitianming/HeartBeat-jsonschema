package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PlatformDepartmentRequest {

    private String parentId;
    @JsonAlias("code")
    private String deptCode;
    @JsonAlias("name")
    private String deptName;
    private String ancestors;
    private Integer deptLevel;
    private String leaderUserId;
    private String phone;
    private String email;
    private Integer sortNo;
    private String status;
}
