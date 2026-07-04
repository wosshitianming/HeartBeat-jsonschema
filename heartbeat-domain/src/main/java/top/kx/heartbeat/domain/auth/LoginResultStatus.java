package top.kx.heartbeat.domain.auth;

/**
 * 登录结果状态。
 */
public enum LoginResultStatus {

    /**
     * 登录成功。
     */
    SUCCESS("SUCCESS"),

    /**
     * 登录失败。
     */
    FAIL("FAIL");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    LoginResultStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
