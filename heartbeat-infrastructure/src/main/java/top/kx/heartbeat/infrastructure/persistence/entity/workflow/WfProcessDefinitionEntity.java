package top.kx.heartbeat.infrastructure.persistence.entity.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流流程定义持久化实体。
 *
 * <p>映射 wf_process_definition 表。</p>
 */
@Data
public class WfProcessDefinitionEntity {

    /**
     * 流程定义主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 流程定义名称。
     */
    private String name;

    /**
     * 流程定义编码。
     */
    private String definitionKey;

    /**
     * 流程定义版本号。
     */
    private Integer versionNo;

    /**
     * 表单结构 JSON。
     */
    private String formSchema;

    /**
     * 流程定义状态。
     */
    private String status;

    /**
     * 部署时间。
     */
    private LocalDateTime deployedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 创建人主键。
     */
    private String createBy;

    /**
     * 更新人主键。
     */
    private String updateBy;
}
