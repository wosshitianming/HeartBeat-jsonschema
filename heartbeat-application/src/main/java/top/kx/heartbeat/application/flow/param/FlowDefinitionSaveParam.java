package top.kx.heartbeat.application.flow.param;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义保存参数（Save Param）
 *
 * @author heartbeat-team
 */
@Data
public class FlowDefinitionSaveParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID（为空时新增，否则更新） */
    private String id;

    /** 流程名称 */
    private String name;

    /** 流程编码 */
    private String code;

    /** 流程描述 */
    private String description;
}