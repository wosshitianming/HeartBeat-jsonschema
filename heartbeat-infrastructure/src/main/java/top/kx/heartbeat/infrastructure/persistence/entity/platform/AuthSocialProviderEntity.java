package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方登录渠道持久化对象（对应表 auth_social_provider）
 * <p>
 * 描述对接的第三方登录渠道（微信/钉钉/GitHub 等），敏感字段以密文存储。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class AuthSocialProviderEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 渠道编码（租户内唯一） */
    private String providerCode;

    /** 渠道名称 */
    private String providerName;

    /** 渠道类型（WECHAT/DINGTALK/GITHUB/...） */
    private String providerType;

    /** 渠道侧 clientId */
    private String clientId;

    /** 渠道侧 appKey（部分渠道专用） */
    private String appKey;

    /** 渠道侧 appSecret 密文 */
    private String appSecretCipher;

    /** 授权端点 */
    private String authorizeUrl;

    /** Token 端点 */
    private String tokenUrl;

    /** 用户信息端点 */
    private String userInfoUrl;

    /** 申请的 scope */
    private String scopes;

    /** 是否启用 */
    private Boolean enabled;

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
