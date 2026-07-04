package top.kx.heartbeat.application.admin.dto;

import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 后台模块数据传输对象。
 *
 * <p>用于向前端返回后台模块菜单及模块元数据。</p>
 */
@Value
public class AdminModuleDTO {

    /**
     * 模块唯一标识。
     */
    String key;

    /**
     * 模块展示名称。
     */
    String name;

    /**
     * 模块所属分类。
     */
    String category;

    /**
     * 模块描述文案。
     */
    String description;

    /**
     * 模块权限前缀。
     */
    String permissionPrefix;

    /**
     * 模块状态编码。
     */
    String status;

    /**
     * 模块支持动作列表。
     */
    List<String> actions;

    /**
     * 模块指标列表。
     */
    List<AdminMetricDTO> metrics;

    /**
     * 模块表格列名列表。
     */
    List<String> columns;

    /**
     * 模块记录列表。
     */
    List<Map<String, String>> records;
}
