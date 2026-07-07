// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.workflow.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class WorkflowDefinitionRequest {

    // 注释：声明当前成员或方法。
    private String name;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias({"definition_key", "key"})
    // 注释：声明当前成员或方法。
    private String definitionKey;
    // 注释：声明当前成员或方法。
    private Integer versionNo;
    // 注释：声明当前成员或方法。
    private Object formSchema;
    // 注释：声明当前成员或方法。
    private String bpmnXml;
// 注释：结束当前代码块。
}
