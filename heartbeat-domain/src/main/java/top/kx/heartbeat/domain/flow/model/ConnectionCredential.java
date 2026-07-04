package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程连接凭证领域模型。
 *
 * <p>用于描述外部系统连接的公开配置、敏感配置和生命周期状态。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionCredential {

    /**
     * 连接凭证标识。
     */
    private String id;

    /**
     * 连接凭证名称。
     */
    private String name;

    /**
     * 连接凭证类型。
     */
    private String type;

    /**
     * 连接公开配置。
     */
    private Map<String, Object> config = new LinkedHashMap<>();

    /**
     * 连接敏感配置。
     */
    private Map<String, Object> secrets = new LinkedHashMap<>();

    /**
     * 连接凭证状态。
     */
    private String status;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 创建时间。
     */
    private Instant createTime;

    /**
     * 更新时间。
     */
    private Instant updateTime;
}
