package top.kx.heartbeat.application.platform.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.application.common.response.RecordResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 账号密码登录响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private AuthTokenResponse tokens;
    private RecordResponse user;
    private List<String> permissions;

    public static LoginResponse of(AuthTokenResponse tokens, RecordResponse user, List<String> permissions) {
        List<String> safePermissions = permissions == null ? Collections.emptyList() : permissions;
        return new LoginResponse(tokens, user, new ArrayList<>(safePermissions));
    }
}
