package top.kx.heartbeat.domain.pay;

/**
 * 支付渠道状态。
 */
public enum PayChannelStatus {

    /**
     * 已启用。
     */
    ACTIVE("ACTIVE"),

    /**
     * 已停用。
     */
    DISABLED("DISABLED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PayChannelStatus(String code) {
        this.code = code;
    }

    /**
     * 按编码解析渠道状态。
     */
    public static PayChannelStatus fromCode(String code) {
        for (PayChannelStatus status : values()) {
            if (status.code.equalsIgnoreCase(String.valueOf(code))) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知支付渠道状态: " + code);
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
