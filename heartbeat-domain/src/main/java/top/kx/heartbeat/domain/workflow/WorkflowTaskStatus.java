package top.kx.heartbeat.domain.workflow;

/**
 * 工作流任务状态。
 */
public enum WorkflowTaskStatus {

    /**
     * 待处理。
     */
    TODO("TODO"),

    /**
     * 已通过。
     */
    APPROVED("APPROVED"),

    /**
     * 已拒绝。
     */
    REJECTED("REJECTED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    WorkflowTaskStatus(String code) {
        this.code = code;
    }

    /**
     * 判断编码是否等于当前状态。
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
