package top.kx.heartbeat.domain.pay;

/**
 * 支付通知业务状态。
 */
public enum PayNotifyStatus {

    /**
     * 签名校验失败。
     */
    SIGN_FAIL("SIGN_FAIL");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PayNotifyStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
