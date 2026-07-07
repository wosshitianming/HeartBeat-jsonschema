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
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> tables = new ArrayList<>();
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try (Connection connection = dataSource.getConnection()) {
            // 读取数据库元数据，用于生成表和字段预览信息。
            DatabaseMetaData metaData = connection.getMetaData();
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String catalog = connection.getCatalog();
            // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
            try (ResultSet resultSet = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                // 持续读取可用数据，直到当前数据源处理完成。
                while (resultSet.next()) {
                    // 计算当前步骤所需的中间值，供后续业务判断使用。
                    String tableName = resultSet.getString("TABLE_NAME");
                    // 根据当前业务条件选择对应处理路径。
                    if (shouldSkipTable(tableName)) {
                        // 跳过当前不需要展示的节点，继续处理下一条数据。
                        continue;
                    }
                    // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
                    Map<String, Object> row = new LinkedHashMap<>();
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("tableName", tableName);
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("comment", resultSet.getString("REMARKS"));
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    tables.add(row);
                }
            }
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("读取数据库表失败", ex);
        }
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        tables.sort((left, right) -> String.valueOf(left.get("tableName")).compareTo(String.valueOf(right.get("tableName"))));
        // 返回已经完成封装的业务结果。
        return RecordResponse.fromMaps(tables);
    }

    @Override
    public List<RecordResponse> listTableColumns(String tableName) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> columns = new ArrayList<>();
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try (Connection connection = dataSource.getConnection()) {
            // 读取数据库元数据，用于生成表和字段预览信息。
            DatabaseMetaData metaData = connection.getMetaData();
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String catalog = connection.getCatalog();
            // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
            try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, "%")) {
                // 持续读取可用数据，直到当前数据源处理完成。
                while (resultSet.next()) {
                    // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
                    Map<String, Object> row = new LinkedHashMap<>();
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("columnName", resultSet.getString("COLUMN_NAME"));
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("columnType", resultSet.getString("TYPE_NAME"));
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("comment", resultSet.getString("REMARKS"));
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    row.put("nullable", "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")));
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    columns.add(row);
                }
            }
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("读取表字段失败: " + tableName, ex);
        }
        // 返回已经完成封装的业务结果。
        return RecordResponse.fromMaps(columns);
    }

    @Override
    public Map<String, String> preview(String tableName, Map<String, Object> options) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> artifacts = new LinkedHashMap<>();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String className = stringValue(options, "className", toClassName(tableName));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        artifacts.put(className + "DO.java", generateEntityStub(className, options));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        artifacts.put(className + "DOExample.java", generateExampleStub(className, options));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        artifacts.put(className + "DOMapper.java", generateMapperStub(className, options));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        artifacts.put(className + "Repository.java", generateRepositoryStub(className, options));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        artifacts.put(className + "Controller.java", generateControllerStub(className, options));
        // 返回已经完成封装的业务结果。
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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "." + stringValue(options, "moduleName", "generated");
        // 返回已经完成封装的业务结果。
        return "package " + pkg + ".persistence.entity.gen;\n\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "import java.util.ArrayList;\nimport java.util.List;\n\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "public class " + className + "DOExample {\n"
                // 创建结果集合，承接后续逐项组装的数据。
                + "    protected List<Criteria> oredCriteria = new ArrayList<>();\n\n"
                // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
                + "    public Criteria createCriteria() {\n"
                // 创建当前流程需要的临时对象，承载后续处理数据。
                + "        Criteria criteria = new Criteria();\n"
                // 加入当前处理结果，供后续批量返回或继续组装。
                + "        oredCriteria.add(criteria);\n"
                // 返回当前分支已经整理好的处理结果。
                + "        return criteria;\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "    }\n\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "    public static class Criteria {\n"
                // 返回当前分支已经整理好的处理结果。
                + "        public Criteria andIdEqualTo(Long value) { return this; }\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "    }\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "}\n";
    }

    private String generateMapperStub(String className, Map<String, Object> options) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String pkg = stringValue(options, "basePackage", "top.kx.heartbeat")
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "." + stringValue(options, "moduleName", "generated");
        // 返回已经完成封装的业务结果。
        return "package " + pkg + ".persistence.mapper.gen;\n\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "import java.util.List;\nimport " + pkg + ".persistence.entity.gen." + className + "DO;\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "import " + pkg + ".persistence.entity.gen." + className + "DOExample;\n\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "public interface " + className + "DOMapper {\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                + "    long countByExample(" + className + "DOExample example);\n"
                // 调用 Mapper 写入数据库，完成当前业务状态变更。
                + "    int insertSelective(" + className + "DO record);\n"
                // 通过 Mapper 查询数据库记录，获取后续转换所需的原始数据。
                + "    List<" + className + "DO> selectByExample(" + className + "DOExample example);\n"
                // 调用 Mapper 写入数据库，完成当前业务状态变更。
                + "    int updateByPrimaryKeySelective(" + className + "DO record);\n"
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
                builder.append(part.substring(1));
            }
        }
        // 返回已经完成封装的业务结果。
        return builder.toString();
    }

    private String stringValue(Map<String, Object> options, String key, String defaultValue) {
        Object value = options == null ? null : options.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
