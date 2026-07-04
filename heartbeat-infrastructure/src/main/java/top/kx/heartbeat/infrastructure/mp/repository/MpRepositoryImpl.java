package top.kx.heartbeat.infrastructure.mp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAccountDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAccountDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAutoReplyDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAutoReplyDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpAccountDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpAutoReplyDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMaterialDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMenuDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公众号应用仓储实现。
 *
 * <p>承接应用层公众号仓储端口，使用 MyBatis Generator 的 Example/Criteria 完成公众号账号、
 * 菜单、素材和自动回复的持久化读写。</p>
 */
@Repository
public class MpRepositoryImpl implements MpRepository {

    /**
     * JSON 序列化组件。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 公众号账号 Mapper。
     */
    @Autowired
    private MpAccountDOMapper accountDOMapper;

    /**
     * 公众号菜单 Mapper。
     */
    @Autowired
    private MpMenuDOMapper menuDOMapper;

    /**
     * 公众号素材 Mapper。
     */
    @Autowired
    private MpMaterialDOMapper materialDOMapper;

    /**
     * 自动回复 Mapper。
     */
    @Autowired
    private MpAutoReplyDOMapper autoReplyDOMapper;

    /**
     * 查询当前租户下的公众号账号列表。
     *
     * @return 公众号账号记录列表。
     */
    @Override
    public List<DomainRecord> listAccounts() {
        // 构造当前租户的账号查询条件。
        MpAccountDOExample example = new MpAccountDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        // 查询账号列表并转换为应用层记录。
        return accountDOMapper.selectByExample(example)
                .stream()
                .map(this::toAccountRecord)
                .collect(Collectors.toList());
    }

    /**
     * 查询公众号账号详情。
     *
     * @param id 公众号账号标识或 AppId。
     * @return 公众号账号记录。
     */
    @Override
    public DomainRecord getAccount(String id) {
        // 支持按主键或 AppId 查询账号。
        return toAccountRecord(requireAccount(id));
    }

    /**
     * 保存公众号账号。
     *
     * @param command 公众号账号保存命令。
     * @return 保存后的公众号账号记录。
     */
    @Override
    public DomainRecord saveAccount(Map<String, Object> command) {
        // 命令携带 id 时执行更新，否则创建新账号。
        MpAccountDO record = findAccountById(longValue(command.get("id"), -1L));
        Date now = new Date();
        if (record == null) {
            // 创建账号持久化对象并写入租户上下文。
            record = new MpAccountDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            // 合并命令字段并插入数据库。
            applyAccount(record, command);
            record.setUpdateTime(now);
            accountDOMapper.insertSelective(record);
            return toAccountRecord(record);
        }
        // 更新已有账号。
        applyAccount(record, command);
        record.setUpdateTime(now);
        accountDOMapper.updateByPrimaryKeySelective(record);
        return toAccountRecord(requireAccount(String.valueOf(record.getId())));
    }

    /**
     * 查询公众号菜单列表。
     *
     * @param accountId 公众号账号标识。
     * @return 公众号菜单记录列表。
     */
    @Override
    public List<DomainRecord> listMenus(String accountId) {
        // 按租户和账号查询菜单。
        MpMenuDOExample example = new MpMenuDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("sort_no ASC, id ASC");
        // 菜单载荷是 BLOB 字段，使用 WithBLOBs 查询。
        return menuDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toMenuRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存公众号菜单。
     *
     * @param command 公众号菜单保存命令。
     * @return 保存后的公众号菜单记录。
     */
    @Override
    public DomainRecord saveMenu(Map<String, Object> command) {
        // 命令携带 id 时更新菜单，否则创建菜单。
        MpMenuDO record = findMenuById(longValue(command.get("id"), -1L));
        Date now = new Date();
        if (record == null) {
            // 创建菜单持久化对象。
            record = new MpMenuDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            // 合并命令字段并插入数据库。
            applyMenu(record, command);
            record.setUpdateTime(now);
            menuDOMapper.insertSelective(record);
            return toMenuRecord(record);
        }
        // 更新已有菜单。
        applyMenu(record, command);
        record.setUpdateTime(now);
        menuDOMapper.updateByPrimaryKeySelective(record);
        return toMenuRecord(record);
    }

    /**
     * 查询公众号素材列表。
     *
     * @param accountId 公众号账号标识。
     * @return 公众号素材记录列表。
     */
    @Override
    public List<DomainRecord> listMaterials(String accountId) {
        // 按租户和账号查询素材。
        MpMaterialDOExample example = new MpMaterialDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("create_time DESC, id DESC");
        // 素材载荷是 BLOB 字段，使用 WithBLOBs 查询。
        return materialDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toMaterialRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存公众号素材。
     *
     * @param command 公众号素材保存命令。
     * @return 保存后的公众号素材记录。
     */
    @Override
    public DomainRecord saveMaterial(Map<String, Object> command) {
        // 命令携带 id 时更新素材，否则创建素材。
        MpMaterialDO record = findMaterialById(longValue(command.get("id"), -1L));
        Date now = new Date();
        if (record == null) {
            // 创建素材持久化对象。
            record = new MpMaterialDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            // 合并命令字段并插入数据库。
            applyMaterial(record, command);
            record.setUpdateTime(now);
            materialDOMapper.insertSelective(record);
            return toMaterialRecord(record);
        }
        // 更新已有素材。
        applyMaterial(record, command);
        record.setUpdateTime(now);
        materialDOMapper.updateByPrimaryKeySelective(record);
        return toMaterialRecord(record);
    }

    /**
     * 查询公众号自动回复列表。
     *
     * @param accountId 公众号账号标识。
     * @return 自动回复记录列表。
     */
    @Override
    public List<DomainRecord> listAutoReplies(String accountId) {
        // 按租户和账号查询自动回复。
        MpAutoReplyDOExample example = new MpAutoReplyDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("sort_no ASC, id ASC");
        // 回复内容是 BLOB 字段，使用 WithBLOBs 查询。
        return autoReplyDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toAutoReplyRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存公众号自动回复。
     *
     * @param command 自动回复保存命令。
     * @return 保存后的自动回复记录。
     */
    @Override
    public DomainRecord saveAutoReply(Map<String, Object> command) {
        // 命令携带 id 时更新自动回复，否则创建自动回复。
        MpAutoReplyDO record = findAutoReplyById(longValue(command.get("id"), -1L));
        Date now = new Date();
        if (record == null) {
            // 创建自动回复持久化对象。
            record = new MpAutoReplyDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            // 合并命令字段并插入数据库。
            applyAutoReply(record, command);
            record.setUpdateTime(now);
            autoReplyDOMapper.insertSelective(record);
            return toAutoReplyRecord(record);
        }
        // 更新已有自动回复。
        applyAutoReply(record, command);
        record.setUpdateTime(now);
        autoReplyDOMapper.updateByPrimaryKeySelective(record);
        return toAutoReplyRecord(record);
    }

    /**
     * 将命令字段合并到公众号账号持久化对象。
     *
     * @param record 公众号账号持久化对象。
     * @param command 账号保存命令。
     */
    private void applyAccount(MpAccountDO record, Map<String, Object> command) {
        // 读取账号基础字段，未传字段沿用原值或默认值。
        record.setName(value(command, "name", value(record.getName(), "公众号账号")));
        record.setAppId(value(command, "appId", value(command, "app_id", value(record.getAppId(), ""))));
        record.setAppSecret(value(command, "appSecret", value(command, "app_secret", value(record.getAppSecret(), ""))));
        record.setToken(value(command, "token", value(record.getToken(), "")));
        record.setAesKey(value(command, "aesKey", value(command, "aes_key", value(record.getAesKey(), ""))));
        record.setStatus(value(command, "status", value(record.getStatus(), "ACTIVE")));
    }

    /**
     * 将命令字段合并到公众号菜单持久化对象。
     *
     * @param record 公众号菜单持久化对象。
     * @param command 菜单保存命令。
     */
    private void applyMenu(MpMenuDO record, Map<String, Object> command) {
        // 兼容驼峰和下划线字段名。
        record.setAccountId(longValue(value(command, "accountId", value(command, "account_id", stringValue(record.getAccountId()))), 0L));
        record.setParentId(longValue(value(command, "parentId", value(command, "parent_id", stringValue(record.getParentId()))), 0L));
        record.setName(value(command, "name", value(record.getName(), "菜单")));
        record.setMenuType(value(command, "menuType", value(command, "type", value(record.getMenuType(), "view"))));
        record.setUrl(value(command, "url", value(record.getUrl(), "")));
        record.setSortNo(intValue(command.get("sortNo"), intValue(record.getSortNo(), 0)));
        record.setStatus(value(command, "status", value(record.getStatus(), "ACTIVE")));
        record.setPayload(jsonValue(command.containsKey("payload") ? command.get("payload") : record.getPayload()));
    }

    /**
     * 将命令字段合并到公众号素材持久化对象。
     *
     * @param record 公众号素材持久化对象。
     * @param command 素材保存命令。
     */
    private void applyMaterial(MpMaterialDO record, Map<String, Object> command) {
        // 兼容驼峰和下划线字段名。
        record.setAccountId(longValue(value(command, "accountId", value(command, "account_id", stringValue(record.getAccountId()))), 0L));
        record.setMaterialType(value(command, "materialType", value(command, "type", value(record.getMaterialType(), "text"))));
        record.setTitle(value(command, "title", value(record.getTitle(), "素材")));
        record.setMediaId(value(command, "mediaId", value(command, "media_id", value(record.getMediaId(), ""))));
        record.setUrl(value(command, "url", value(record.getUrl(), "")));
        record.setStatus(value(command, "status", value(record.getStatus(), "ACTIVE")));
        record.setPayload(jsonValue(command.containsKey("payload") ? command.get("payload") : record.getPayload()));
    }

    /**
     * 将命令字段合并到自动回复持久化对象。
     *
     * @param record 自动回复持久化对象。
     * @param command 自动回复保存命令。
     */
    private void applyAutoReply(MpAutoReplyDO record, Map<String, Object> command) {
        // 读取匹配规则、回复类型和回复内容。
        record.setAccountId(longValue(value(command, "accountId", value(command, "account_id", stringValue(record.getAccountId()))), 0L));
        record.setKeyword(value(command, "keyword", value(record.getKeyword(), "")));
        record.setMatchType(value(command, "matchType", value(command, "match_type", value(record.getMatchType(), "EXACT"))));
        record.setReplyType(value(command, "replyType", value(command, "reply_type", value(record.getReplyType(), "TEXT"))));
        record.setSortNo(intValue(command.get("sortNo"), intValue(record.getSortNo(), 0)));
        record.setStatus(value(command, "status", value(record.getStatus(), "ACTIVE")));
        record.setReplyContent(textValue(command.containsKey("replyContent") ? command.get("replyContent") : command.get("content"), record.getReplyContent()));
    }

    /**
     * 查询必须存在的公众号账号。
     *
     * @param id 公众号账号主键或 AppId。
     * @return 公众号账号持久化对象。
     */
    private MpAccountDO requireAccount(String id) {
        // 优先按主键查询。
        MpAccountDO record = findAccountById(longValue(id, -1L));
        if (record != null) {
            return record;
        }
        // 主键未命中时按 AppId 查询。
        MpAccountDOExample example = new MpAccountDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(id);
        List<MpAccountDO> records = accountDOMapper.selectByExample(example);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("公众号账号不存在: " + id);
        }
        return records.get(0);
    }

    /**
     * 按主键查询当前租户下的公众号账号。
     *
     * @param id 账号主键。
     * @return 账号持久化对象，未命中时返回 null。
     */
    private MpAccountDO findAccountById(long id) {
        // 非法主键直接视为未命中。
        if (id <= 0) {
            return null;
        }
        // 主键查询后校验租户，避免越权读取。
        MpAccountDO record = accountDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 按主键查询当前租户下的公众号菜单。
     *
     * @param id 菜单主键。
     * @return 菜单持久化对象，未命中时返回 null。
     */
    private MpMenuDO findMenuById(long id) {
        // 非法主键直接视为未命中。
        if (id <= 0) {
            return null;
        }
        // 主键查询后校验租户。
        MpMenuDO record = menuDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 按主键查询当前租户下的公众号素材。
     *
     * @param id 素材主键。
     * @return 素材持久化对象，未命中时返回 null。
     */
    private MpMaterialDO findMaterialById(long id) {
        // 非法主键直接视为未命中。
        if (id <= 0) {
            return null;
        }
        // 主键查询后校验租户。
        MpMaterialDO record = materialDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 按主键查询当前租户下的自动回复。
     *
     * @param id 自动回复主键。
     * @return 自动回复持久化对象，未命中时返回 null。
     */
    private MpAutoReplyDO findAutoReplyById(long id) {
        // 非法主键直接视为未命中。
        if (id <= 0) {
            return null;
        }
        // 主键查询后校验租户。
        MpAutoReplyDO record = autoReplyDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 将公众号账号持久化对象转换为应用层动态记录。
     *
     * @param entity 公众号账号持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toAccountRecord(MpAccountDO entity) {
        // 使用有序 Map 保持接口输出字段稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("appId", entity.getAppId());
        row.put("appSecret", entity.getAppSecret());
        row.put("token", entity.getToken());
        row.put("aesKey", entity.getAesKey());
        row.put("status", entity.getStatus());
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 将公众号菜单持久化对象转换为应用层动态记录。
     *
     * @param entity 公众号菜单持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toMenuRecord(MpMenuDO entity) {
        // 组装菜单接口字段。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("parentId", stringValue(entity.getParentId()));
        row.put("name", entity.getName());
        row.put("menuType", entity.getMenuType());
        row.put("url", entity.getUrl());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("payload", readJson(entity.getPayload()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 将公众号素材持久化对象转换为应用层动态记录。
     *
     * @param entity 公众号素材持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toMaterialRecord(MpMaterialDO entity) {
        // 组装素材接口字段。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("materialType", entity.getMaterialType());
        row.put("title", entity.getTitle());
        row.put("mediaId", entity.getMediaId());
        row.put("url", entity.getUrl());
        row.put("status", entity.getStatus());
        row.put("payload", readJson(entity.getPayload()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 将自动回复持久化对象转换为应用层动态记录。
     *
     * @param entity 自动回复持久化对象。
     * @return 应用层动态记录。
     */
    private DomainRecord toAutoReplyRecord(MpAutoReplyDO entity) {
        // 组装自动回复接口字段。
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("keyword", entity.getKeyword());
        row.put("matchType", entity.getMatchType());
        row.put("replyType", entity.getReplyType());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("replyContent", entity.getReplyContent());
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 读取 JSON 字符串。
     *
     * @param json JSON 字符串。
     * @return JSON 节点。
     */
    private JsonNode readJson(String json) {
        try {
            // 空值统一按空对象处理，减少前端判空。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 解析失败", ex);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 原始对象。
     * @return JSON 字符串。
     */
    private String jsonValue(Object value) {
        try {
            // 空值统一保存为空对象。
            if (value == null) {
                return "{}";
            }
            // 字符串视为已经序列化的 JSON。
            if (value instanceof String) {
                String text = ((String) value).trim();
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            // 其他对象交给 Jackson 序列化。
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON 序列化失败", ex);
        }
    }

    /**
     * 将回复内容转换为文本。
     *
     * @param value 原始回复内容。
     * @param defaultValue 默认回复内容。
     * @return 回复文本。
     */
    private String textValue(Object value, String defaultValue) {
        // 未传回复内容时沿用默认值。
        if (value == null) {
            return defaultValue == null ? "" : defaultValue;
        }
        // 字符串直接作为回复内容。
        if (value instanceof String) {
            return (String) value;
        }
        // 结构化回复内容保存为 JSON 字符串。
        return jsonValue(value);
    }

    /**
     * 从命令中读取字符串字段。
     *
     * @param command 命令对象。
     * @param key 字段名。
     * @param defaultValue 默认值。
     * @return 字符串字段值。
     */
    private String value(Map<String, Object> command, String key, String defaultValue) {
        return command.containsKey(key) ? value(command.get(key), defaultValue) : defaultValue;
    }

    /**
     * 将对象转换为非空字符串。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 字符串值。
     */
    private String value(Object raw, String defaultValue) {
        String text = stringValue(raw);
        return StringUtils.isBlank(text) ? defaultValue : text;
    }

    /**
     * 将对象转换为整数。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 整数值。
     */
    private int intValue(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为长整型。
     *
     * @param raw 原始值。
     * @param defaultValue 默认值。
     * @return 长整型值。
     */
    private long longValue(Object raw, long defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为去空白字符串。
     *
     * @param value 原始值。
     * @return 字符串值。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识，未绑定租户时使用默认租户。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
