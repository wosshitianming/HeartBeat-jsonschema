package top.kx.heartbeat.application.platform.request;

import lombok.Data;

@Data
public class PlatformLoginRequest {

    private String username;
    private String password;
}
