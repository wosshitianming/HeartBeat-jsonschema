// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.workflow.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class WorkflowStartRequest {

    // 注释：声明当前成员或方法。
    private String businessKey;
    // 注释：声明当前成员或方法。
    private String title;
    // 注释：声明当前成员或方法。
    private String initiatorId;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("assignee_id")
    // 注释：声明当前成员或方法。
    private String assigneeId;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("approver_id")
    // 注释：声明当前成员或方法。
    private String approverId;
    // 注释：声明当前成员或方法。
    private Object payload;
// 注释：结束当前代码块。
}
