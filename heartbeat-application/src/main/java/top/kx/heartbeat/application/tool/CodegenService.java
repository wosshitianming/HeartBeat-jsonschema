package top.kx.heartbeat.application.tool;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.application.tool.port.MybatisGeneratorPreviewer;
import top.kx.heartbeat.domain.tool.CodegenMetadataRepository;
import top.kx.heartbeat.domain.tool.model.GeneratedColumn;
import top.kx.heartbeat.domain.tool.model.GeneratedTable;

import javax.annotation.Resource;
import java.util.*;

/**
 * MyBatis Generator 代码生成应用服务。
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

    public List<RecordResponse> listDatabaseTables() {
        return mybatisGeneratorPreviewer.listDatabaseTables();
    }

    public List<RecordResponse> listImportedTables() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GeneratedTable table : codegenMetadataRepository.findAllTables()) {
            rows.add(toMap(table));
        }
        return RecordResponse.fromMaps(rows);
    }

    @Transactional
    public RecordResponse importTable(String tableName) {
        List<RecordResponse> columns = mybatisGeneratorPreviewer.listTableColumns(tableName);
        Map<String, Object> options = buildOptions(tableName);
        GeneratedTable table = codegenMetadataRepository.saveTable(GeneratedTable.builder()
                .tableName(tableName)
                .tableComment("MyBatis Generator import")
                .className(String.valueOf(options.get("className")))
                .moduleName(String.valueOf(options.get("moduleName")))
                .basePackage(String.valueOf(options.get("basePackage")))
                .resourceKey(String.valueOf(options.get("resourceKey")))
                .optionsJson(writeJson(options))
                .status("ENABLED")
                .build());

        List<GeneratedColumn> generatedColumns = new ArrayList<>();
        int sortNo = 0;
        for (RecordResponse columnRecord : columns) {
            Map<String, Object> column = columnRecord.toMap();
            String columnName = String.valueOf(column.get("columnName"));
            String dataType = String.valueOf(column.get("columnType"));
            generatedColumns.add(GeneratedColumn.builder()
                    .tenantId(table.getTenantId())
                    .tableId(table.getId())
                    .columnName(columnName)
                    .columnComment(stringValue(column.get("comment")))
                    .dataType(dataType)
                    .javaType(toJavaType(dataType))
                    .javaField(toJavaField(columnName))
                    .primaryKey(false)
                    .autoIncrement(false)
                    .nullable(Boolean.TRUE.equals(column.get("nullable")))
                    .sortNo(sortNo++)
                    .build());
        }
        codegenMetadataRepository.replaceColumns(table.getId(), generatedColumns);
        syncMenu(options);
        return RecordResponse.from(toMap(table));
    }

    public Map<String, String> preview(String tableId) {
        Map<String, Object> table = findImportedTableRequired(tableId);
        return mybatisGeneratorPreviewer.preview(resolveTableName(table), buildOptionsFromRecord(table));
    }

    public byte[] download(String tableId) {
        Map<String, Object> table = findImportedTableRequired(tableId);
        return mybatisGeneratorPreviewer.downloadZip(resolveTableName(table), buildOptionsFromRecord(table));
    }

    private Map<String, Object> findImportedTableRequired(String tableId) {
        Optional<RecordResponse> tableOptional = listImportedTables().stream()
                .filter(item -> tableId.equals(String.valueOf(item.get("id"))))
                .findFirst();
        return tableOptional.map(RecordResponse::toMap)
                .orElseThrow(() -> new IllegalArgumentException("生成配置不存在: " + tableId));
    }

    private Map<String, Object> buildOptions(String tableName) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("tableName", tableName);
        options.put("className", toClassName(tableName));
        options.put("moduleName", "generated");
        options.put("basePackage", "top.kx.heartbeat");
        options.put("resourceKey", toResourceKey(tableName));
        return options;
    }

    private void syncMenu(Map<String, Object> options) {
        String resourceKey = String.valueOf(options.get("resourceKey"));
        String className = String.valueOf(options.get("className"));
        String permissionPrefix = "biz:" + resourceKey.replace('-', ':');
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("parentId", "biz");
        menu.put("menuCode", permissionPrefix);
        menu.put("type", "MENU");
        menu.put("name", className + "管理");
        menu.put("path", "/biz/generated/" + resourceKey);
        menu.put("component", "generated/" + className + "Page");
        menu.put("permission", permissionPrefix + ":list");
        menu.put("icon", "code");
        menu.put("sortNo", 90);
        RecordResponse createdMenu = adminPlatformService.createMenu(menu);
        String menuId = String.valueOf(createdMenu.get("id"));
        syncButton(menuId, className + "新增", permissionPrefix + ":add", 1);
        syncButton(menuId, className + "修改", permissionPrefix + ":edit", 2);
        syncButton(menuId, className + "删除", permissionPrefix + ":remove", 3);
    }

    private void syncButton(String parentId, String name, String permission, int sortNo) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("parentId", parentId);
        button.put("menuCode", permission);
        button.put("type", "BUTTON");
        button.put("name", name);
        button.put("permission", permission);
        button.put("sortNo", sortNo);
        adminPlatformService.createMenu(button);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildOptionsFromRecord(Map<String, Object> table) {
        Object payload = table.get("payload");
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        if (payload != null) {
            try {
                return objectMapper.readValue(String.valueOf(payload), Map.class);
            } catch (JsonProcessingException ignored) {
            }
        }
        return buildOptions(String.valueOf(table.get("name")));
    }

    private String resolveTableName(Map<String, Object> table) {
        Map<String, Object> options = buildOptionsFromRecord(table);
        Object tableName = options.get("tableName");
        if (StringUtils.isNotBlank(tableName == null ? null : String.valueOf(tableName))) {
            return String.valueOf(tableName);
        }
        return String.valueOf(table.get("name"));
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化生成配置失败", ex);
        }
    }

    private String toClassName(String tableName) {
        String[] parts = tableName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.isEmpty(part)) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }

    private String toResourceKey(String tableName) {
        String normalized = tableName.toLowerCase();
        if (normalized.startsWith("sys_")) {
            normalized = normalized.substring(4);
        } else if (normalized.startsWith("hb_")) {
            normalized = normalized.substring(3);
        }
        return normalized.replace('_', '-');
    }

    private Map<String, Object> toMap(GeneratedTable table) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(table.getId()));
        row.put("tenantId", String.valueOf(table.getTenantId()));
        row.put("name", table.getTableName());
        row.put("code", table.getClassName());
        row.put("description", table.getTableComment());
        row.put("payload", table.getOptionsJson());
        row.put("status", table.getStatus());
        return row;
    }

    private String toJavaType(String dataType) {
        String normalized = dataType == null ? "" : dataType.toUpperCase();
        if (normalized.contains("BIGINT")) {
            return "Long";
        }
        if (normalized.contains("INT")) {
            return "Integer";
        }
        if (normalized.contains("DECIMAL") || normalized.contains("NUMERIC")) {
            return "BigDecimal";
        }
        if (normalized.contains("DATE") || normalized.contains("TIME")) {
            return "LocalDateTime";
        }
        if (normalized.contains("BOOL") || normalized.contains("BIT")) {
            return "Boolean";
        }
        return "String";
    }

    private String toJavaField(String columnName) {
        StringBuilder field = new StringBuilder();
        boolean upperNext = false;
        for (char character : columnName.toLowerCase().toCharArray()) {
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                field.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                field.append(character);
            }
        }
        return field.toString();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
