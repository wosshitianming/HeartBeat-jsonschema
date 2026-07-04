package top.kx.heartbeat.application.flow.param;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义查询参数（Query Param）
 *
 * @author heartbeat-team
 */
@Data
public class FlowDefinitionQueryParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 名称模糊匹配 */
    private String nameLike;

    /** 编码精确匹配 */
    private String codeEqual;

    /** 状态精确匹配 */
    private String statusEqual;

    /** 排序列 */
    private String orderByColumn;

    /** 排序方向（asc/desc） */
    private String orderByDirection;
}