package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Date;

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
