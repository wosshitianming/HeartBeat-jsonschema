package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Date;

/**
 * 承载平台管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PlatformUserRequest {

    private String deptId;
    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    @JsonAlias("avatar")
    private String avatarUrl;
    private String password;
    private String passwordHash;
    private String passwordAlgo;
    private Date passwordUpdateTime;
    private String gender;
    private String userType;
    private String status;
}
