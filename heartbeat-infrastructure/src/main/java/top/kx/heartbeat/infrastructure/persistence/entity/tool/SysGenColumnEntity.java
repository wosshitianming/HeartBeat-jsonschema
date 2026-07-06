package top.kx.heartbeat.infrastructure.persistence.entity.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码生成列配置持久化对象（对应表 sys_gen_column）
 * <p>
 * 描述对一张业务表中某一列的生成策略：Java 字段名、类型、是否主键、是否必填等。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysGenColumnEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属代码生成表配置 ID */
    private Long genTableId;

    /** 列名 */
    private String columnName;

    /** 列注释 */
    private String columnComment;

    /** 数据库类型 */
    private String dataType;

    /** 映射到的 Java 类型 */
    private String javaType;

    /** 映射到的 Java 字段名 */
    private String javaField;

    /** 是否主键 */
    private Boolean primaryKey;

    /** 是否自增 */
    private Boolean autoIncrement;

    /** 是否可空 */
    private Boolean nullable;

    /** 排序号 */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
