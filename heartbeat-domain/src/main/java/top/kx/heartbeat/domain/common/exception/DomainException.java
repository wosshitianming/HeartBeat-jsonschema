package top.kx.heartbeat.domain.common.exception;

/**
 * 领域异常：表示违反了领域业务规则（不变量）。
 *
 * <p>由聚合根、值对象、领域服务在校验失败时抛出，携带稳定的错误码以便上层统一处理与国际化。
 * 注意：领域层不感知 HTTP，错误码到响应状态的映射在接口层完成。
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码，建议使用稳定字符串常量（如 USER_EMAIL_INVALID）。
     */
    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
