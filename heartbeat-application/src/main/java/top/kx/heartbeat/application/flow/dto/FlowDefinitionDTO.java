package top.kx.heartbeat.application.flow.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义 DTO（Data Transfer Object）
 * <p>
 * 领域层与应用层之间传输的扁平对象，与 DO 字段一一对应（除敏感字段外）。
 * 严禁在此对象上编写业务方法，仅作为数据载体；DTO 通过 MapStruct 与 DO/VO/Param 互转。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class FlowDefinitionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程定义主键 ID */
    private String id;

    /** 流程名称 */
    private String name;

    /** 流程编码（业务唯一键） */
    private String code;

    /** 流程描述 */
    private String description;

    /** 流程状态 */
    private String status;

    /** 当前激活版本号 */
    private Integer activeVersionNo;

    /** DSL JSON 内容 */
    private String dslJson;

    /** 创建人账号 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 ID */
    private String createBy;

    /** 更新者 ID */
    private String updateBy;
}
