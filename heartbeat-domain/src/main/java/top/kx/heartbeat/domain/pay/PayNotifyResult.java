package top.kx.heartbeat.domain.pay;

/**
 * 支付通知验签结果。
 */
public enum PayNotifyResult {

    /**
     * 验签成功。
     */
    SUCCESS("SUCCESS"),

    /**
     * 验签失败。
     */
    FAIL("FAIL");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PayNotifyResult(String code) {
        this.code = code;
    }

    /**
     * 判断编码是否等于当前枚举。
     */
    public boolean matches(String value) {
        return code.equalsIgnoreCase(String.valueOf(value));
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
