package top.kx.heartbeat.domain.workflow;

/**
 * 工作流定义状态。
 */
public enum WorkflowDefinitionStatus {

    /**
     * 草稿状态。
     */
    DRAFT("DRAFT"),

    /**
     * 已发布状态。
     */
    DEPLOYED("DEPLOYED");

    /**
     * 持久化编码。
     */
    private final String code;

    /**
     * 绑定持久化编码。
     */
    WorkflowDefinitionStatus(String code) {
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
