package top.kx.heartbeat.application.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.application.platform.request.PlatformMenuRequest;
import top.kx.heartbeat.application.tool.port.MybatisGeneratorPreviewer;
import top.kx.heartbeat.domain.tool.CodegenMetadataRepository;
import top.kx.heartbeat.domain.tool.model.GeneratedColumn;
import top.kx.heartbeat.domain.tool.model.GeneratedTable;

import javax.annotation.Resource;
import java.util.*;


/**
 * 编排代码生成应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class CodegenService {

    @Resource
    private MybatisGeneratorPreviewer mybatisGeneratorPreviewer;
    @Resource
    private CodegenMetadataRepository codegenMetadataRepository;
    @Resource
    private PlatformAdministrationService adminPlatformService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调代码生成相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDatabaseTables() {
        return mybatisGeneratorPreviewer.listDatabaseTables();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调代码生成相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listImportedTables() {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> rows = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (GeneratedTable table : codegenMetadataRepository.findAllTables()) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            rows.add(toMap(table));
        }
        // 返回已经完成封装的业务结果。
        return RecordResponse.fromMaps(rows);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param tableName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse importTable(String tableName) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<RecordResponse> columns = mybatisGeneratorPreviewer.listTableColumns(tableName);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> options = buildOptions(tableName);
        // 追加代码或文本片段，逐步生成最终内容。
        GeneratedTable table = codegenMetadataRepository.saveTable(GeneratedTable.builder()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tableName(tableName)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tableComment("MyBatis Generator import")
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .className(String.valueOf(options.get("className")))
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .moduleName(String.valueOf(options.get("moduleName")))
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .basePackage(String.valueOf(options.get("basePackage")))
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                .resourceKey(String.valueOf(options.get("resourceKey")))
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                .optionsJson(writeJson(options))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .status("ENABLED")
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .build());

        // 创建结果集合，承接后续逐项组装的数据。
        List<GeneratedColumn> generatedColumns = new ArrayList<>();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        int sortNo = 0;
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (RecordResponse columnRecord : columns) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Map<String, Object> column = columnRecord.toMap();
            // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
            String columnName = String.valueOf(column.get("columnName"));
            // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
            String dataType = String.valueOf(column.get("columnType"));
            // 追加代码或文本片段，逐步生成最终内容。
            generatedColumns.add(GeneratedColumn.builder()
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .tenantId(table.getTenantId())
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .tableId(table.getId())
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .columnName(columnName)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .columnComment(stringValue(column.get("comment")))
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .dataType(dataType)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .javaType(toJavaType(dataType))
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .javaField(toJavaField(columnName))
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .primaryKey(false)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .autoIncrement(false)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .nullable(Boolean.TRUE.equals(column.get("nullable")))
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .sortNo(sortNo++)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .build());
        }
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        codegenMetadataRepository.replaceColumns(table.getId(), generatedColumns);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        syncMenu(options);
        // 返回已经完成封装的业务结果。
        return RecordResponse.from(toMap(table));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param tableId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public Map<String, String> preview(String tableId) {
        Map<String, Object> table = findImportedTableRequired(tableId);
        return mybatisGeneratorPreviewer.preview(resolveTableName(table), buildOptionsFromRecord(table));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param tableId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public byte[] download(String tableId) {
        Map<String, Object> table = findImportedTableRequired(tableId);
        return mybatisGeneratorPreviewer.downloadZip(resolveTableName(table), buildOptionsFromRecord(table));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调代码生成相关仓储和领域规则。
     *
     * @param tableId 业务记录标识。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> findImportedTableRequired(String tableId) {
        Optional<RecordResponse> tableOptional = listImportedTables().stream()
                .filter(item -> tableId.equals(String.valueOf(item.get("id"))))
                .findFirst();
        return tableOptional.map(RecordResponse::toMap)
                .orElseThrow(() -> new IllegalArgumentException("生成配置不存在: " + tableId));
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调代码生成相关仓储和领域规则。
     *
     * @param tableName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> buildOptions(String tableName) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> options = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        options.put("tableName", tableName);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        options.put("className", toClassName(tableName));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        options.put("moduleName", "generated");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        options.put("basePackage", "top.kx.heartbeat");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        options.put("resourceKey", toResourceKey(tableName));
        // 返回已经完成封装的业务结果。
        return options;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param options 业务处理所需参数。
     */
    private void syncMenu(Map<String, Object> options) {
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String resourceKey = String.valueOf(options.get("resourceKey"));
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String className = String.valueOf(options.get("className"));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String permissionPrefix = "biz:" + resourceKey.replace('-', ':');
        // 创建下游写入请求对象，集中承载本次业务处理结果。
        PlatformMenuRequest menu = new PlatformMenuRequest();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setMenuCode(permissionPrefix);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setMenuType("MENU");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setMenuName(className + "管理");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setRoutePath("/biz/generated/" + resourceKey);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setComponentPath("generated/" + className + "Page");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setPermissionMode(permissionPrefix + ":list");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setIcon("code");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        menu.setSortNo(90);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        RecordResponse createdMenu = adminPlatformService.createMenu(menu);
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String menuId = String.valueOf(createdMenu.get("id"));
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        syncButton(menuId, className + "新增", permissionPrefix + ":add", 1);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        syncButton(menuId, className + "修改", permissionPrefix + ":edit", 2);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        syncButton(menuId, className + "删除", permissionPrefix + ":remove", 3);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param parentId 业务记录标识。
     * @param name 业务处理所需参数。
     * @param permission 业务处理所需参数。
     * @param sortNo 业务处理所需参数。
     */
    private void syncButton(String parentId, String name, String permission, int sortNo) {
        // 创建下游写入请求对象，集中承载本次业务处理结果。
        PlatformMenuRequest button = new PlatformMenuRequest();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setParentId(parentId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setMenuCode(permission);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setMenuType("BUTTON");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setMenuName(name);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setPermissionMode(permission);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        button.setSortNo(sortNo);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        adminPlatformService.createMenu(button);
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调代码生成相关仓储和领域规则。
     *
     * @param table 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildOptionsFromRecord(Map<String, Object> table) {
        // 读取扩展参数载体，为后续动态处理准备数据。
        Object payload = table.get("payload");
        // 根据当前业务条件选择对应处理路径。
        if (payload instanceof Map) {
            // 返回已经完成封装的业务结果。
            return (Map<String, Object>) payload;
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (payload != null) {
            // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
            try {
                // 返回已经完成封装的业务结果。
                return objectMapper.readValue(String.valueOf(payload), Map.class);
            } catch (JsonProcessingException ignored) {
            }
        }
        // 返回已经完成封装的业务结果。
        return buildOptions(String.valueOf(table.get("name")));
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调代码生成相关仓储和领域规则。
     *
     * @param table 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String resolveTableName(Map<String, Object> table) {
        Map<String, Object> options = buildOptionsFromRecord(table);
        Object tableName = options.get("tableName");
        if (StringUtils.isNotBlank(tableName == null ? null : String.valueOf(tableName))) {
            return String.valueOf(tableName);
        }
        return String.valueOf(table.get("name"));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调代码生成相关仓储和领域规则。
     *
     * @param payload 支付渠道回调原文。
     * @return 处理后的业务结果。
     */
    private String writeJson(Map<String, Object> payload) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("序列化生成配置失败", ex);
        }
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调代码生成相关仓储和领域规则。
     *
     * @param tableName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String toClassName(String tableName) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String[] parts = tableName.split("_");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        StringBuilder builder = new StringBuilder();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String part : parts) {
            // 校验关键文本参数，防止无效输入继续向后流转。
            if (StringUtils.isEmpty(part)) {
                // 跳过当前不需要展示的节点，继续处理下一条数据。
                continue;
            }
            // 追加当前片段，逐步拼接最终结果。
            builder.append(Character.toUpperCase(part.charAt(0)));
            // 根据当前业务条件选择对应处理路径。
            if (part.length() > 1) {
                // 追加当前片段，逐步拼接最终结果。
                builder.append(part.substring(1).toLowerCase());
            }
        }
        // 返回已经完成封装的业务结果。
        return builder.toString();
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调代码生成相关仓储和领域规则。
     *
     * @param tableName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String toResourceKey(String tableName) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String normalized = tableName.toLowerCase();
        // 根据当前业务条件选择对应处理路径。
        if (normalized.startsWith("sys_")) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            normalized = normalized.substring(4);
        } else if (normalized.startsWith("hb_")) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            normalized = normalized.substring(3);
        }
        // 返回已经完成封装的业务结果。
        return normalized.replace('_', '-');
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调代码生成相关仓储和领域规则。
     *
     * @param table 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> toMap(GeneratedTable table) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(table.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(table.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("name", table.getTableName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("code", table.getClassName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("description", table.getTableComment());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("payload", table.getOptionsJson());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", table.getStatus());
        // 返回已经完成封装的业务结果。
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调代码生成相关仓储和领域规则。
     *
     * @param dataType 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String toJavaType(String dataType) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String normalized = dataType == null ? "" : dataType.toUpperCase();
        // 根据当前业务条件选择对应处理路径。
        if (normalized.contains("BIGINT")) {
            // 返回已经完成封装的业务结果。
            return "Long";
        }
        // 根据当前业务条件选择对应处理路径。
        if (normalized.contains("INT")) {
            // 返回已经完成封装的业务结果。
            return "Integer";
        }
        // 根据当前业务条件选择对应处理路径。
        if (normalized.contains("DECIMAL") || normalized.contains("NUMERIC")) {
            // 返回已经完成封装的业务结果。
            return "BigDecimal";
        }
        // 根据当前业务条件选择对应处理路径。
        if (normalized.contains("DATE") || normalized.contains("TIME")) {
            // 返回已经完成封装的业务结果。
            return "LocalDateTime";
        }
        // 根据当前业务条件选择对应处理路径。
        if (normalized.contains("BOOL") || normalized.contains("BIT")) {
            // 返回已经完成封装的业务结果。
            return "Boolean";
        }
        // 返回已经完成封装的业务结果。
        return "String";
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调代码生成相关仓储和领域规则。
     *
     * @param columnName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String toJavaField(String columnName) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        StringBuilder field = new StringBuilder();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        boolean upperNext = false;
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (char character : columnName.toLowerCase().toCharArray()) {
            // 根据当前业务条件选择对应处理路径。
            if (character == '_') {
                // 计算当前分支的中间结果，供后续判断或组装使用。
                upperNext = true;
            } else if (upperNext) {
                // 追加当前片段，逐步拼接最终结果。
                field.append(Character.toUpperCase(character));
                // 计算当前分支的中间结果，供后续判断或组装使用。
                upperNext = false;
            } else {
                // 追加当前片段，逐步拼接最终结果。
                field.append(character);
            }
        }
        // 返回已经完成封装的业务结果。
        return field.toString();
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调代码生成相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
