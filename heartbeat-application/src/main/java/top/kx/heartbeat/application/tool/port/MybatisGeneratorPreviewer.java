package top.kx.heartbeat.application.tool.port;

import top.kx.heartbeat.application.common.response.RecordResponse;

import java.util.List;
import java.util.Map;

/**
 * MyBatis Generator preview port for database metadata and generated artifacts.
 */
public interface MybatisGeneratorPreviewer {

    /**
     * 列出当前数据源中可导入的业务表（排除 QRTZ_、ACT_ 等系统表）。
     */
    List<RecordResponse> listDatabaseTables();

    /**
     * 读取单表列元数据。
     */
    List<RecordResponse> listTableColumns(String tableName);

    /**
     * 预览生成代码（文件名 -> 内容）。
     */
    Map<String, String> preview(String tableName, Map<String, Object> options);

    /**
     * 生成代码 ZIP 包字节流。
     */
    byte[] downloadZip(String tableName, Map<String, Object> options);
}
