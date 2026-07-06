package top.kx.heartbeat.infrastructure.codegen;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.tool.port.MybatisGeneratorPreviewer;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JDBC metadata reader for MyBatis Generator previews.
 * <p>
 * 实际生成通过 MyBatis Generator 工具完成，本类只读取数据库表/字段信息并提供预览入口。
 * </p>
 *
 * @author heartbeat-team
 */
@Component
public class MybatisGeneratorPreviewEngine implements MybatisGeneratorPreviewer {

    @Resource
    private DataSource dataSource;

    @Override
    public List<RecordResponse> listDatabaseTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet resultSet = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    if (shouldSkipTable(tableName)) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("tableName", tableName);
                    row.put("comment", resultSet.getString("REMARKS"));
                    tables.add(row);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取数据库表失败", ex);
        }
        tables.sort((left, right) -> String.valueOf(left.get("tableName")).compareTo(String.valueOf(right.get("tableName"))));
        return RecordResponse.fromMaps(tables);
    }

    @Override
    public List<RecordResponse> listTableColumns(String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, "%")) {
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("columnName", resultSet.getString("COLUMN_NAME"));
                    row.put("columnType", resultSet.getString("TYPE_NAME"));
                    row.put("comment", resultSet.getString("REMARKS"));
                    row.put("nullable", "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")));
                    columns.add(row);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取表字段失败: " + tableName, ex);
        }
        return RecordResponse.fromMaps(columns);
    }

    @Override
    public Map<String, String> preview(String tableName, Map<String, Object> options) {
        Map<String, String> artifacts = new LinkedHashMap<>();
        String className = stringValue(options, "className", toClassName(tableName));
        artifacts.put(className + "DO.java", generateEntityStub(className, options));
        artifacts.put(className + "DOExample.java", generateExampleStub(className, options));
        artifacts.put(className + "DOMapper.java", generateMapperStub(className, options));
        artifacts.put(className + "Repository.java", generateRepositoryStub(className, options));
        artifacts.put(className + "Controller.java", generateControllerStub(className, options));
        return artifacts;
    }

    @Override
    public byte[] downloadZip(String tableName, Map<String, Object> options) {
        throw new UnsupportedOperationException("MyBatis Generator 代码生成请使用 CLI 工具，本接口仅提供预览");
    }

    private String generateEntityStub(String className, Map<String, Object> options) {
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                + "." + stringValue(options, "moduleName", "generated");
        return "package " + pkg + ".persistence.entity.gen;\n\npublic class "
                + className + "DO {\n\n    private Long id;\n\n    public Long getId() { return id; }\n\n    public void setId(Long id) { this.id = id; }\n}\n";
    }

    private String generateExampleStub(String className, Map<String, Object> options) {
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                + "." + stringValue(options, "moduleName", "generated");
        return "package " + pkg + ".persistence.entity.gen;\n\n"
                + "import java.util.ArrayList;\nimport java.util.List;\n\n"
                + "public class " + className + "DOExample {\n"
                + "    protected List<Criteria> oredCriteria = new ArrayList<>();\n\n"
                + "    public Criteria createCriteria() {\n"
                + "        Criteria criteria = new Criteria();\n"
                + "        oredCriteria.add(criteria);\n"
                + "        return criteria;\n"
                + "    }\n\n"
                + "    public static class Criteria {\n"
                + "        public Criteria andIdEqualTo(Long value) { return this; }\n"
                + "    }\n"
                + "}\n";
    }

    private String generateMapperStub(String className, Map<String, Object> options) {
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                + "." + stringValue(options, "moduleName", "generated");
        return "package " + pkg + ".persistence.mapper.gen;\n\n"
                + "import java.util.List;\nimport " + pkg + ".persistence.entity.gen." + className + "DO;\n"
                + "import " + pkg + ".persistence.entity.gen." + className + "DOExample;\n\n"
                + "public interface " + className + "DOMapper {\n"
                + "    long countByExample(" + className + "DOExample example);\n"
                + "    int insertSelective(" + className + "DO record);\n"
                + "    List<" + className + "DO> selectByExample(" + className + "DOExample example);\n"
                + "    int updateByPrimaryKeySelective(" + className + "DO record);\n"
                + "}\n";
    }

    private String generateRepositoryStub(String className, Map<String, Object> options) {
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                + "." + stringValue(options, "moduleName", "generated");
        return "package " + pkg + ".repository;\n\nimport org.springframework.stereotype.Repository;\n\n@Repository\npublic class "
                + className + "Repository {}\n";
    }

    private String generateControllerStub(String className, Map<String, Object> options) {
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                + "." + stringValue(options, "moduleName", "generated");
        return "package " + pkg + ".controller;\n\nimport org.springframework.web.bind.annotation.RestController;\n\n@RestController\npublic class "
                + className + "Controller {}\n";
    }

    private boolean shouldSkipTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String upper = tableName.toUpperCase();
        return upper.startsWith("QRTZ_")
                || upper.startsWith("ACT_")
                || upper.equals("USERS")
                || upper.startsWith("STRUCTURE_");
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
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String stringValue(Map<String, Object> options, String key, String defaultValue) {
        Object value = options == null ? null : options.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
