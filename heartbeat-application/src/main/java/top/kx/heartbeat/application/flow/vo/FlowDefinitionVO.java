package top.kx.heartbeat.application.flow.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义 VO（View Object）
 *
 * @author heartbeat-team
 */
@Data
public class FlowDefinitionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程定义主键 ID */
    private String id;

    /** 流程名称 */
    private String name;

    /** 流程编码 */
    private String code;

    /** 流程描述 */
    private String description;

    /** 流程状态文本 */
    private String statusText;

    /** 当前激活版本号 */
    private Integer activeVersionNo;

    /** DSL JSON 内容 */
    private String dslJson;

    /** 创建人账号 */
    private String createdBy;

    /** 创建时间 */
    private String createTime;

    /** 更新时间 */
    private String updateTime;
}