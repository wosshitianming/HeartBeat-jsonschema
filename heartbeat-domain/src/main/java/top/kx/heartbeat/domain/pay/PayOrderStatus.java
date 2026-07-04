package top.kx.heartbeat.domain.pay;

/**
 * 支付订单状态。
 */
public enum PayOrderStatus {

    /**
     * 订单已创建。
     */
    CREATED("CREATED"),

    /**
     * 订单支付中。
     */
    PAYING("PAYING"),

    /**
     * 订单已支付。
     */
    PAID("PAID"),

    /**
     * 订单已关闭。
     */
    CLOSED("CLOSED"),

    /**
     * 订单已部分退款。
     */
    PART_REFUNDED("PART_REFUNDED"),

    /**
     * 订单已全额退款。
     */
    REFUNDED("REFUNDED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PayOrderStatus(String code) {
        this.code = code;
    }

    /**
     * 按编码解析订单状态。
     */
    public static PayOrderStatus fromCode(String code) {
        for (PayOrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(String.valueOf(code))) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知支付订单状态: " + code);
    }

    /**
     * 判断是否允许流转到目标状态。
     */
    public boolean canTransitTo(PayOrderStatus targetStatus) {
        switch (this) {
            case CREATED:
                return targetStatus == PAYING || targetStatus == CLOSED;
            case PAYING:
                return targetStatus == PAID || targetStatus == CLOSED;
            case PAID:
                return targetStatus == PART_REFUNDED;
            case PART_REFUNDED:
                return targetStatus == REFUNDED;
            default:
                return false;
        }
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
