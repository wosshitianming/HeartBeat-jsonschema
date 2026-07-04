package top.kx.heartbeat.infrastructure.persistence.entity.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码生成表导入配置持久化对象（对应表 sys_gen_table）
 * <p>
 * 描述对一张业务表的生成策略：模块名、基础包名、类名、生成选项等。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysGenTableEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 表名 */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 生成的类名 */
    private String className;

    /** 所属模块名 */
    private String moduleName;

    /** 生成代码基础包 */
    private String basePackage;

    /** 资源标识 */
    private String resourceKey;

    /** 生成选项 JSON（模板/路径/作者等） */
    private String optionsJson;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除标记 */
    private Long deleteMarker;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private Long createBy;

    /** 更新者 */
    private Long updateBy;
}
