package top.kx.heartbeat.domain.user;

/**
 * 用户上下文的领域错误码常量。
 *
 * <p>错误码是领域与上层之间稳定的契约，便于日志检索、国际化以及前端按码处理。
 */
public final class UserErrorCode {

    private UserErrorCode() {
    }

    public static final String EMAIL_INVALID = "USER_EMAIL_INVALID";
    public static final String USERNAME_INVALID = "USER_USERNAME_INVALID";
    public static final String EMAIL_DUPLICATED = "USER_EMAIL_DUPLICATED";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_DISABLED = "USER_ALREADY_DISABLED";
}
