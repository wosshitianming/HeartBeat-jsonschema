package top.kx.heartbeat.domain.flow.validation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程 DSL 校验问题。
 *
 * <p>用于描述单个流程定义校验失败点。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowValidationIssue {

    /**
     * 问题编码。
     */
    private String code;

    /**
     * 问题路径。
     */
    private String path;

    /**
     * 问题消息。
     */
    private String message;
}
