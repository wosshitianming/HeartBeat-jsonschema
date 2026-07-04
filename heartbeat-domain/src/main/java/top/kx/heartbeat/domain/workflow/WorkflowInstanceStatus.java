package top.kx.heartbeat.domain.workflow;

/**
 * 工作流实例状态。
 */
public enum WorkflowInstanceStatus {

    /**
     * 运行中。
     */
    RUNNING("RUNNING"),

    /**
     * 审批通过。
     */
    APPROVED("APPROVED"),

    /**
     * 审批拒绝。
     */
    REJECTED("REJECTED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    WorkflowInstanceStatus(String code) {
        this.code = code;
    }

    /**
     * 返回持久化编码。
     */
    public String getCode() {
        return code;
    }
}
