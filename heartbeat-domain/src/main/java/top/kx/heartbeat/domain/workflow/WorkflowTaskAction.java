package top.kx.heartbeat.domain.workflow;

/**
 * 工作流任务动作。
 */
public enum WorkflowTaskAction {

    /**
     * 审批通过动作。
     */
    APPROVE("APPROVE", WorkflowTaskStatus.APPROVED, WorkflowInstanceStatus.APPROVED),

    /**
     * 审批拒绝动作。
     */
    REJECT("REJECT", WorkflowTaskStatus.REJECTED, WorkflowInstanceStatus.REJECTED);

    /**
     * 动作编码。
     */
    private final String code;

    /**
     * 动作完成后的任务状态。
     */
    private final WorkflowTaskStatus taskStatus;

    /**
     * 动作完成后的实例状态。
     */
    private final WorkflowInstanceStatus instanceStatus;

    /**
     * 绑定动作编码与状态结果。
     */
    WorkflowTaskAction(String code, WorkflowTaskStatus taskStatus, WorkflowInstanceStatus instanceStatus) {
        this.code = code;
        this.taskStatus = taskStatus;
        this.instanceStatus = instanceStatus;
    }

    /**
     * 按编码解析任务动作。
     */
    public static WorkflowTaskAction fromCode(String code) {
        for (WorkflowTaskAction action : values()) {
            if (action.code.equalsIgnoreCase(String.valueOf(code))) {
                return action;
            }
        }
        throw new IllegalArgumentException("未知工作流任务动作: " + code);
    }

    /**
     * 返回动作编码。
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回任务状态编码。
     */
    public String taskStatusCode() {
        return taskStatus.getCode();
    }

    /**
     * 返回实例状态编码。
     */
    public String instanceStatusCode() {
        return instanceStatus.getCode();
    }
}
