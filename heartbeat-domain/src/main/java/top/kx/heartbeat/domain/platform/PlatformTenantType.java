package top.kx.heartbeat.domain.platform;

/**
 * 平台租户与套餐类型。
 */
public enum PlatformTenantType {

    /**
     * 企业版。
     */
    ENTERPRISE("ENTERPRISE");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    PlatformTenantType(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
