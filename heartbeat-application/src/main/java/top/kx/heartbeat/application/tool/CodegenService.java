// 注释：声明当前文件所属的包路径。
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
 * MyBatis Generator 代码生成应用服务。
 */

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class CodegenService {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MybatisGeneratorPreviewer mybatisGeneratorPreviewer;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private CodegenMetadataRepository codegenMetadataRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformAdministrationService adminPlatformService;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ObjectMapper objectMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDatabaseTables() {
        // 注释：返回当前处理结果。
        return mybatisGeneratorPreviewer.listDatabaseTables();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listImportedTables() {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> rows = new ArrayList<>();
        // 注释：遍历当前数据集合。
        for (GeneratedTable table : codegenMetadataRepository.findAllTables()) {
            // 注释：执行当前代码行。
            rows.add(toMap(table));
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(rows);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse importTable(String tableName) {
        // 注释：设置或计算当前变量值。
        List<RecordResponse> columns = mybatisGeneratorPreviewer.listTableColumns(tableName);
        // 注释：设置或计算当前变量值。
        Map<String, Object> options = buildOptions(tableName);
        // 注释：设置或计算当前变量值。
        GeneratedTable table = codegenMetadataRepository.saveTable(GeneratedTable.builder()
                // 注释：继续当前链式调用。
                .tableName(tableName)
                // 注释：继续当前链式调用。
                .tableComment("MyBatis Generator import")
                // 注释：继续当前链式调用。
                .className(String.valueOf(options.get("className")))
                // 注释：继续当前链式调用。
                .moduleName(String.valueOf(options.get("moduleName")))
                // 注释：继续当前链式调用。
                .basePackage(String.valueOf(options.get("basePackage")))
                // 注释：继续当前链式调用。
                .resourceKey(String.valueOf(options.get("resourceKey")))
                // 注释：继续当前链式调用。
                .optionsJson(writeJson(options))
                // 注释：继续当前链式调用。
                .status("ENABLED")
                // 注释：继续当前链式调用。
                .build());

        // 注释：设置或计算当前变量值。
        List<GeneratedColumn> generatedColumns = new ArrayList<>();
        // 注释：设置或计算当前变量值。
        int sortNo = 0;
        // 注释：遍历当前数据集合。
        for (RecordResponse columnRecord : columns) {
            // 注释：设置或计算当前变量值。
            Map<String, Object> column = columnRecord.toMap();
            // 注释：设置或计算当前变量值。
            String columnName = String.valueOf(column.get("columnName"));
            // 注释：设置或计算当前变量值。
            String dataType = String.valueOf(column.get("columnType"));
            // 注释：执行当前代码行。
            generatedColumns.add(GeneratedColumn.builder()
                    // 注释：继续当前链式调用。
                    .tenantId(table.getTenantId())
                    // 注释：继续当前链式调用。
                    .tableId(table.getId())
                    // 注释：继续当前链式调用。
                    .columnName(columnName)
                    // 注释：继续当前链式调用。
                    .columnComment(stringValue(column.get("comment")))
                    // 注释：继续当前链式调用。
                    .dataType(dataType)
                    // 注释：继续当前链式调用。
                    .javaType(toJavaType(dataType))
                    // 注释：继续当前链式调用。
                    .javaField(toJavaField(columnName))
                    // 注释：继续当前链式调用。
                    .primaryKey(false)
                    // 注释：继续当前链式调用。
                    .autoIncrement(false)
                    // 注释：继续当前链式调用。
                    .nullable(Boolean.TRUE.equals(column.get("nullable")))
                    // 注释：继续当前链式调用。
                    .sortNo(sortNo++)
                    // 注释：继续当前链式调用。
                    .build());
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        codegenMetadataRepository.replaceColumns(table.getId(), generatedColumns);
        // 注释：执行当前代码行。
        syncMenu(options);
        // 注释：返回当前处理结果。
        return RecordResponse.from(toMap(table));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public Map<String, String> preview(String tableId) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> table = findImportedTableRequired(tableId);
        // 注释：返回当前处理结果。
        return mybatisGeneratorPreviewer.preview(resolveTableName(table), buildOptionsFromRecord(table));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public byte[] download(String tableId) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> table = findImportedTableRequired(tableId);
        // 注释：返回当前处理结果。
        return mybatisGeneratorPreviewer.downloadZip(resolveTableName(table), buildOptionsFromRecord(table));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> findImportedTableRequired(String tableId) {
        // 注释：设置或计算当前变量值。
        Optional<RecordResponse> tableOptional = listImportedTables().stream()
                // 注释：继续当前链式调用。
                .filter(item -> tableId.equals(String.valueOf(item.get("id"))))
                // 注释：继续当前链式调用。
                .findFirst();
        // 注释：返回当前处理结果。
        return tableOptional.map(RecordResponse::toMap)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("生成配置不存在: " + tableId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> buildOptions(String tableName) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> options = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        options.put("tableName", tableName);
        // 注释：执行当前代码行。
        options.put("className", toClassName(tableName));
        // 注释：执行当前代码行。
        options.put("moduleName", "generated");
        // 注释：执行当前代码行。
        options.put("basePackage", "top.kx.heartbeat");
        // 注释：执行当前代码行。
        options.put("resourceKey", toResourceKey(tableName));
        // 注释：返回当前处理结果。
        return options;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void syncMenu(Map<String, Object> options) {
        // 注释：设置或计算当前变量值。
        String resourceKey = String.valueOf(options.get("resourceKey"));
        // 注释：设置或计算当前变量值。
        String className = String.valueOf(options.get("className"));
        // 注释：设置或计算当前变量值。
        String permissionPrefix = "biz:" + resourceKey.replace('-', ':');
        // 注释：设置或计算当前变量值。
        PlatformMenuRequest menu = new PlatformMenuRequest();
        // 注释：执行当前代码行。
        menu.setMenuCode(permissionPrefix);
        // 注释：执行当前代码行。
        menu.setMenuType("MENU");
        // 注释：执行当前代码行。
        menu.setMenuName(className + "管理");
        // 注释：执行当前代码行。
        menu.setRoutePath("/biz/generated/" + resourceKey);
        // 注释：执行当前代码行。
        menu.setComponentPath("generated/" + className + "Page");
        // 注释：执行当前代码行。
        menu.setPermissionMode(permissionPrefix + ":list");
        // 注释：执行当前代码行。
        menu.setIcon("code");
        // 注释：执行当前代码行。
        menu.setSortNo(90);
        // 注释：设置或计算当前变量值。
        RecordResponse createdMenu = adminPlatformService.createMenu(menu);
        // 注释：设置或计算当前变量值。
        String menuId = String.valueOf(createdMenu.get("id"));
        // 注释：执行当前代码行。
        syncButton(menuId, className + "新增", permissionPrefix + ":add", 1);
        // 注释：执行当前代码行。
        syncButton(menuId, className + "修改", permissionPrefix + ":edit", 2);
        // 注释：执行当前代码行。
        syncButton(menuId, className + "删除", permissionPrefix + ":remove", 3);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void syncButton(String parentId, String name, String permission, int sortNo) {
        // 注释：设置或计算当前变量值。
        PlatformMenuRequest button = new PlatformMenuRequest();
        // 注释：执行当前代码行。
        button.setParentId(parentId);
        // 注释：执行当前代码行。
        button.setMenuCode(permission);
        // 注释：执行当前代码行。
        button.setMenuType("BUTTON");
        // 注释：执行当前代码行。
        button.setMenuName(name);
        // 注释：执行当前代码行。
        button.setPermissionMode(permission);
        // 注释：执行当前代码行。
        button.setSortNo(sortNo);
        // 注释：执行当前代码行。
        adminPlatformService.createMenu(button);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildOptionsFromRecord(Map<String, Object> table) {
        // 注释：设置或计算当前变量值。
        Object payload = table.get("payload");
        // 注释：判断当前业务条件。
        if (payload instanceof Map) {
            // 注释：返回当前处理结果。
            return (Map<String, Object>) payload;
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (payload != null) {
            // 注释：开始执行可能抛出异常的逻辑。
            try {
                // 注释：声明当前类。
                return objectMapper.readValue(String.valueOf(payload), Map.class);
                // 注释：捕获并处理当前异常。
            } catch (JsonProcessingException ignored) {
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return buildOptions(String.valueOf(table.get("name")));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String resolveTableName(Map<String, Object> table) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> options = buildOptionsFromRecord(table);
        // 注释：设置或计算当前变量值。
        Object tableName = options.get("tableName");
        // 注释：判断当前业务条件。
        if (StringUtils.isNotBlank(tableName == null ? null : String.valueOf(tableName))) {
            // 注释：返回当前处理结果。
            return String.valueOf(tableName);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return String.valueOf(table.get("name"));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String writeJson(Map<String, Object> payload) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.writeValueAsString(payload);
            // 注释：捕获并处理当前异常。
        } catch (JsonProcessingException ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalStateException("序列化生成配置失败", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String toClassName(String tableName) {
        // 注释：设置或计算当前变量值。
        String[] parts = tableName.split("_");
        // 注释：设置或计算当前变量值。
        StringBuilder builder = new StringBuilder();
        // 注释：遍历当前数据集合。
        for (String part : parts) {
            // 注释：判断当前业务条件。
            if (StringUtils.isEmpty(part)) {
                // 注释：执行当前代码行。
                continue;
                // 注释：结束当前代码块。
            }
            // 注释：执行当前代码行。
            builder.append(Character.toUpperCase(part.charAt(0)));
            // 注释：判断当前业务条件。
            if (part.length() > 1) {
                // 注释：执行当前代码行。
                builder.append(part.substring(1).toLowerCase());
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return builder.toString();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String toResourceKey(String tableName) {
        // 注释：设置或计算当前变量值。
        String normalized = tableName.toLowerCase();
        // 注释：判断当前业务条件。
        if (normalized.startsWith("sys_")) {
            // 注释：设置或计算当前变量值。
            normalized = normalized.substring(4);
            // 注释：处理条件不满足时的分支。
        } else if (normalized.startsWith("hb_")) {
            // 注释：设置或计算当前变量值。
            normalized = normalized.substring(3);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return normalized.replace('_', '-');
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> toMap(GeneratedTable table) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(table.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(table.getTenantId()));
        // 注释：执行当前代码行。
        row.put("name", table.getTableName());
        // 注释：执行当前代码行。
        row.put("code", table.getClassName());
        // 注释：执行当前代码行。
        row.put("description", table.getTableComment());
        // 注释：执行当前代码行。
        row.put("payload", table.getOptionsJson());
        // 注释：执行当前代码行。
        row.put("status", table.getStatus());
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String toJavaType(String dataType) {
        // 注释：设置或计算当前变量值。
        String normalized = dataType == null ? "" : dataType.toUpperCase();
        // 注释：判断当前业务条件。
        if (normalized.contains("BIGINT")) {
            // 注释：返回当前处理结果。
            return "Long";
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (normalized.contains("INT")) {
            // 注释：返回当前处理结果。
            return "Integer";
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (normalized.contains("DECIMAL") || normalized.contains("NUMERIC")) {
            // 注释：返回当前处理结果。
            return "BigDecimal";
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (normalized.contains("DATE") || normalized.contains("TIME")) {
            // 注释：返回当前处理结果。
            return "LocalDateTime";
            // 注释：结束当前代码块。
        }
        // 注释：判断当前业务条件。
        if (normalized.contains("BOOL") || normalized.contains("BIT")) {
            // 注释：返回当前处理结果。
            return "Boolean";
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return "String";
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String toJavaField(String columnName) {
        // 注释：设置或计算当前变量值。
        StringBuilder field = new StringBuilder();
        // 注释：设置或计算当前变量值。
        boolean upperNext = false;
        // 注释：遍历当前数据集合。
        for (char character : columnName.toLowerCase().toCharArray()) {
            // 注释：判断当前业务条件。
            if (character == '_') {
                // 注释：设置或计算当前变量值。
                upperNext = true;
                // 注释：处理条件不满足时的分支。
            } else if (upperNext) {
                // 注释：执行当前代码行。
                field.append(Character.toUpperCase(character));
                // 注释：设置或计算当前变量值。
                upperNext = false;
                // 注释：处理条件不满足时的分支。
            } else {
                // 注释：执行当前代码行。
                field.append(character);
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return field.toString();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value);
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
