package top.kx.heartbeat.infrastructure.tool.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.tool.CodegenMetadataRepository;
import top.kx.heartbeat.domain.tool.model.GeneratedColumn;
import top.kx.heartbeat.domain.tool.model.GeneratedTable;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysGenColumnDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysGenColumnDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysGenTableDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysGenTableDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysGenColumnDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysGenTableDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码生成元数据仓储实现。
 * <p>
 * 负责将代码生成领域模型保存到 MBG 生成的 sys_gen_table 和 sys_gen_column 表对象。
 * </p>
 */
@Repository
public class CodegenMetadataRepositoryImpl implements CodegenMetadataRepository {

    /**
     * 默认租户标识。
     */
    private static final long DEFAULT_TENANT_ID = 1L;

    /**
     * 默认操作人标识。
     */
    private static final long DEFAULT_OPERATOR_ID = 1L;

    /**
     * 代码生成表配置 MBG Mapper。
     */
    @Resource
    private SysGenTableDOMapper tableMapper;

    /**
     * 代码生成字段配置 MBG Mapper。
     */
    @Resource
    private SysGenColumnDOMapper columnMapper;

    /**
     * 查询当前租户下的全部代码生成表配置。
     *
     * @return 代码生成表配置列表
     */
    @Override
    public List<GeneratedTable> findAllTables() {
        // 创建代码生成表查询条件对象。
        SysGenTableDOExample example = new SysGenTableDOExample();
        // 限定当前租户下未逻辑删除的表配置。
        example.createCriteria()
                .andTenantIdEqualTo(currentTenantId())
                .andDeleteMarkerEqualTo(0L);
        // 按主键倒序返回最新配置。
        example.setOrderByClause("id DESC");
        // 查询表配置并转换为领域模型列表。
        return tableMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toDomainTable)
                .collect(Collectors.toList());
    }

    /**
     * 保存代码生成表配置。
     *
     * @param table 代码生成表配置
     * @return 保存后的代码生成表配置
     */
    @Override
    public GeneratedTable saveTable(GeneratedTable table) {
        // 查询当前租户下已经存在的表配置。
        SysGenTableDO existing = findExistingTable(table);
        // 将领域表配置转换为 MBG 表对象。
        SysGenTableDO row = toTableDO(table);
        // 生成本次保存的统一时间。
        Date now = new Date();
        // 写入当前租户标识。
        row.setTenantId(currentTenantId());
        // 标记为未逻辑删除。
        row.setDeleteMarker(0L);
        // 新增时初始化版本号，更新时沿用已有版本号。
        row.setVersion(existing == null ? 0 : existing.getVersion());
        // 新增时写入创建时间，更新时保留原创建时间。
        row.setCreateTime(existing == null ? now : existing.getCreateTime());
        // 写入更新时间。
        row.setUpdateTime(now);
        // 新增时写入默认创建人，更新时保留原创建人。
        row.setCreateBy(existing == null ? DEFAULT_OPERATOR_ID : existing.getCreateBy());
        // 写入默认更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 判断表配置是否已经存在。
        if (existing == null) {
            // 新增代码生成表配置。
            tableMapper.insertSelective(row);
        } else {
            // 设置已有表配置主键。
            row.setId(existing.getId());
            // 更新代码生成表配置。
            tableMapper.updateByPrimaryKeySelective(row);
        }
        // 返回保存后的领域表配置。
        return toDomainTable(row);
    }

    /**
     * 替换代码生成字段配置。
     *
     * @param tableId 表配置标识
     * @param columns 字段配置列表
     */
    @Override
    public void replaceColumns(Long tableId, List<GeneratedColumn> columns) {
        // 创建代码生成字段删除条件对象。
        SysGenColumnDOExample example = new SysGenColumnDOExample();
        // 限定当前租户下指定表配置的字段。
        example.createCriteria()
                .andTenantIdEqualTo(currentTenantId())
                .andGenTableIdEqualTo(tableId);
        // 删除旧的字段配置。
        columnMapper.deleteByExample(example);
        // 遍历新的字段配置列表。
        for (GeneratedColumn column : columns) {
            // 插入新的字段配置。
            columnMapper.insertSelective(toColumnDO(tableId, column));
        }
    }

    /**
     * 查询已有表配置。
     *
     * @param table 代码生成表配置
     * @return 已有表配置
     */
    private SysGenTableDO findExistingTable(GeneratedTable table) {
        // 创建代码生成表查询条件对象。
        SysGenTableDOExample example = new SysGenTableDOExample();
        // 限定当前租户下未逻辑删除的表配置。
        SysGenTableDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(currentTenantId())
                .andDeleteMarkerEqualTo(0L);
        // 优先按主键定位已有表配置。
        if (table.getId() != null) {
            // 添加主键匹配条件。
            criteria.andIdEqualTo(table.getId());
        } else {
            // 无主键时按物理表名定位已有表配置。
            criteria.andTableNameEqualTo(table.getTableName());
        }
        // 查询符合条件的表配置。
        List<SysGenTableDO> rows = tableMapper.selectByExampleWithBLOBs(example);
        // 返回第一条已有配置，没有时返回空。
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 转换为领域表配置。
     *
     * @param row MBG 表配置对象
     * @return 领域表配置
     */
    private GeneratedTable toDomainTable(SysGenTableDO row) {
        // 构建领域表配置对象。
        return GeneratedTable.builder()
                .id(row.getId())
                .tenantId(row.getTenantId())
                .tableName(row.getTableName())
                .tableComment(row.getTableComment())
                .className(row.getClassName())
                .moduleName(row.getModuleName())
                .basePackage(row.getBasePackage())
                .resourceKey(row.getResourceKey())
                .optionsJson(row.getOptionsJson())
                .status(row.getStatus())
                .build();
    }

    /**
     * 转换为 MBG 表配置对象。
     *
     * @param table 领域表配置
     * @return MBG 表配置对象
     */
    private SysGenTableDO toTableDO(GeneratedTable table) {
        // 创建 MBG 表配置对象。
        SysGenTableDO row = new SysGenTableDO();
        // 写入表配置主键。
        row.setId(table.getId());
        // 写入物理表名。
        row.setTableName(table.getTableName());
        // 写入表说明。
        row.setTableComment(table.getTableComment());
        // 写入 Java 类名。
        row.setClassName(table.getClassName());
        // 写入模块名。
        row.setModuleName(table.getModuleName());
        // 写入基础包名。
        row.setBasePackage(table.getBasePackage());
        // 写入资源键。
        row.setResourceKey(table.getResourceKey());
        // 写入代码生成扩展选项。
        row.setOptionsJson(table.getOptionsJson());
        // 写入表配置状态。
        row.setStatus(table.getStatus());
        // 返回 MBG 表配置对象。
        return row;
    }

    /**
     * 转换为 MBG 字段配置对象。
     *
     * @param tableId 表配置标识
     * @param column  字段配置
     * @return MBG 字段配置对象
     */
    private SysGenColumnDO toColumnDO(Long tableId, GeneratedColumn column) {
        // 生成本次字段配置的统一时间。
        Date now = new Date();
        // 创建 MBG 字段配置对象。
        SysGenColumnDO row = new SysGenColumnDO();
        // 写入当前租户标识。
        row.setTenantId(currentTenantId());
        // 写入所属表配置标识。
        row.setGenTableId(tableId);
        // 写入数据库字段名。
        row.setColumnName(column.getColumnName());
        // 写入数据库字段说明。
        row.setColumnComment(column.getColumnComment());
        // 写入数据库字段类型。
        row.setDataType(column.getDataType());
        // 写入 Java 类型。
        row.setJavaType(column.getJavaType());
        // 写入 Java 字段名。
        row.setJavaField(column.getJavaField());
        // 写入是否主键。
        row.setPrimaryKey(column.isPrimaryKey());
        // 写入是否自增。
        row.setAutoIncrement(column.isAutoIncrement());
        // 写入是否允许为空。
        row.setNullable(column.isNullable());
        // 写入字段排序号。
        row.setSortNo(column.getSortNo());
        // 写入创建时间。
        row.setCreateTime(now);
        // 写入更新时间。
        row.setUpdateTime(now);
        // 写入默认创建人。
        row.setCreateBy(DEFAULT_OPERATOR_ID);
        // 写入默认更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 返回 MBG 字段配置对象。
        return row;
    }

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识
     */
    private Long currentTenantId() {
        // 读取线程上下文中的租户标识。
        Long tenantId = TenantContext.getTenantId();
        // 上下文为空时回退到系统默认租户。
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
