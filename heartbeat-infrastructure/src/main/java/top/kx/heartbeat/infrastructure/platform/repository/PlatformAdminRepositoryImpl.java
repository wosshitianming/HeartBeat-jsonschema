package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformAdminRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.*;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthOauthClientDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSessionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialBindingDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialProviderDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.*;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PlatformAdminRepositoryImpl implements PlatformAdminRepository {

    @Resource
    private SysUserDOMapper userMapper;
    @Resource
    private SysUserPreferenceDOMapper userPreferenceMapper;
    @Resource
    private SysUserRoleDOMapper userRoleMapper;
    @Resource
    private SysRoleDOMapper roleMapper;
    @Resource
    private SysRolePermissionDOMapper rolePermissionMapper;
    @Resource
    private SysPermissionDOMapper permissionMapper;
    @Resource
    private SysMenuDOMapper menuMapper;
    @Resource
    private SysMenuPermissionDOMapper menuPermissionMapper;
    @Resource
    private SysRoleDeptDOMapper roleDeptMapper;
    @Resource
    private SysDeptDOMapper deptMapper;
    @Resource
    private SysConfigDOMapper configMapper;
    @Resource
    private AuthSocialProviderDOMapper socialProviderMapper;
    @Resource
    private AuthSocialBindingDOMapper socialBindingMapper;
    @Resource
    private SysLoginLogDOMapper loginLogMapper;
    @Resource
    private SysTenantDOMapper tenantMapper;
    @Resource
    private SysPostDOMapper postMapper;
    @Resource
    private SysDictTypeDOMapper dictTypeMapper;
    @Resource
    private SysDictItemDOMapper dictItemMapper;
    @Resource
    private SysNoticeDOMapper noticeMapper;
    @Resource
    private SysOperLogDOMapper operLogMapper;
    @Resource
    private AuthSessionDOMapper sessionMapper;
    @Resource
    private AuthOauthClientDOMapper oauthClientMapper;

    @Override
    public Optional<DomainRecord> findUserByUsername(String username) {
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria().andUsernameEqualTo(username);
        return first(userMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public Optional<DomainRecord> findUserById(String userId) {
        Long id = longValue(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(userMapper.selectByPrimaryKey(id)).map(this::record);
    }

    @Override
    public Optional<DomainRecord> findUserPreference(String userId, String preferenceKey) {
        Long id = longValue(userId);
        if (id == null) {
            return Optional.empty();
        }
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        return first(userPreferenceMapper.selectByExampleWithBLOBs(example)).map(this::record);
    }

    @Override
    public DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue) {
        Long id = longValue(userId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid user id: " + userId);
        }
        SysUserPreferenceDOExample example = new SysUserPreferenceDOExample();
        example.createCriteria().andUserIdEqualTo(id).andPreferenceKeyEqualTo(preferenceKey);
        Optional<SysUserPreferenceDO> existing = first(userPreferenceMapper.selectByExampleWithBLOBs(example));
        SysUserPreferenceDO row = existing.orElseGet(SysUserPreferenceDO::new);
        row.setTenantId(tenantId());
        row.setUserId(id);
        row.setPreferenceKey(preferenceKey);
        row.setPreferenceValue(preferenceValue);
        row.setValueType("STRING");
        touch(row, !existing.isPresent());
        if (existing.isPresent()) {
            userPreferenceMapper.updateByPrimaryKeySelective(row);
        } else {
            userPreferenceMapper.insertSelective(row);
        }
        return record(row);
    }

    @Override
    public List<String> listPermissionsByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRolePermissionDOExample rolePermissionExample = new SysRolePermissionDOExample();
        rolePermissionExample.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        List<Long> permissionIds = rolePermissionMapper.selectByExample(rolePermissionExample)
                .stream()
                .map(SysRolePermissionDO::getPermissionId)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysPermissionDOExample permissionExample = new SysPermissionDOExample();
        permissionExample.createCriteria().andIdIn(permissionIds);
        return permissionMapper.selectByExample(permissionExample)
                .stream()
                .map(SysPermissionDO::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listRoleCodesByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        return roleMapper.selectByExample(example)
                .stream()
                .map(SysRoleDO::getRoleCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listDataScopesByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDOExample example = new SysRoleDOExample();
        example.createCriteria().andIdIn(new ArrayList<>(roleIds));
        return roleMapper.selectByExample(example)
                .stream()
                .map(SysRoleDO::getDataScope)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listCustomDeptIdsByUserId(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRoleDeptDOExample example = new SysRoleDeptDOExample();
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        return roleDeptMapper.selectByExample(example)
                .stream()
                .map(SysRoleDeptDO::getDeptId)
                .filter(value -> value != null)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainRecord> listMenus() {
        SysMenuDOExample example = new SysMenuDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(menuMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listAuthorizedMenus(String userId) {
        Set<Long> roleIds = roleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = menuIdsByPermissionIds(permissionIdsByRoleIds(roleIds));
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysMenuDOExample menuExample = new SysMenuDOExample();
        menuExample.createCriteria().andIdIn(menuIds);
        menuExample.setOrderByClause("sort_no ASC, id ASC");
        return records(menuMapper.selectByExample(menuExample));
    }

    @Override
    public DomainRecord createMenu(Map<String, Object> command) {
        return create(menuMapper, new SysMenuDO(), command);
    }

    @Override
    public DomainRecord updateMenu(String id, Map<String, Object> command) {
        return update(menuMapper, new SysMenuDO(), id, command);
    }

    @Override
    public void deleteMenu(String id) {
        delete(menuMapper, id);
    }

    @Override
    public boolean roleExists(String roleId) {
        Long id = longValue(roleId);
        return id != null && roleMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public List<String> listRoleMenuIds(String roleId) {
        Long id = longValue(roleId);
        if (id == null) {
            return Collections.emptyList();
        }
        return menuIdsByPermissionIds(permissionIdsByRoleIds(Collections.singleton(id)))
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public void saveRoleMenus(String roleId, List<String> menuIds) {
        Long id = longValue(roleId);
        if (id == null) {
            return;
        }
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        example.createCriteria().andRoleIdEqualTo(id);
        rolePermissionMapper.deleteByExample(example);
        List<Long> parsedMenuIds = menuIds.stream()
                .map(this::longValue)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
        if (parsedMenuIds.isEmpty()) {
            return;
        }
        SysMenuPermissionDOExample menuPermissionExample = new SysMenuPermissionDOExample();
        menuPermissionExample.createCriteria().andMenuIdIn(parsedMenuIds);
        List<Long> permissionIds = menuPermissionMapper.selectByExample(menuPermissionExample)
                .stream()
                .map(SysMenuPermissionDO::getPermissionId)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
        Date now = new Date();
        for (Long permissionId : permissionIds) {
            SysRolePermissionDO row = new SysRolePermissionDO();
            row.setTenantId(tenantId());
            row.setRoleId(id);
            row.setPermissionId(permissionId);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rolePermissionMapper.insertSelective(row);
        }
    }

    @Override
    public List<DomainRecord> listUsers() {
        SysUserDOExample example = new SysUserDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(userMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createUser(Map<String, Object> command) {
        return create(userMapper, new SysUserDO(), command);
    }

    @Override
    public DomainRecord updateUser(String id, Map<String, Object> command) {
        return update(userMapper, new SysUserDO(), id, command);
    }

    @Override
    public void deleteUser(String id) {
        delete(userMapper, id);
    }

    @Override
    public List<DomainRecord> listDepartments() {
        SysDeptDOExample example = new SysDeptDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(deptMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createDepartment(Map<String, Object> command) {
        return create(deptMapper, new SysDeptDO(), command);
    }

    @Override
    public DomainRecord updateDepartment(String id, Map<String, Object> command) {
        return update(deptMapper, new SysDeptDO(), id, command);
    }

    @Override
    public void deleteDepartment(String id) {
        delete(deptMapper, id);
    }

    @Override
    public List<DomainRecord> listRoles() {
        SysRoleDOExample example = new SysRoleDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(roleMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createRole(Map<String, Object> command) {
        return create(roleMapper, new SysRoleDO(), command);
    }

    @Override
    public DomainRecord updateRole(String id, Map<String, Object> command) {
        return update(roleMapper, new SysRoleDO(), id, command);
    }

    @Override
    public void deleteRole(String id) {
        delete(roleMapper, id);
    }

    @Override
    public List<DomainRecord> listConfigurations() {
        return records(configMapper.selectByExample(new SysConfigDOExample()));
    }

    @Override
    public DomainRecord createConfiguration(Map<String, Object> command) {
        return create(configMapper, new SysConfigDO(), command);
    }

    @Override
    public DomainRecord updateConfiguration(String id, Map<String, Object> command) {
        return update(configMapper, new SysConfigDO(), id, command);
    }

    @Override
    public void deleteConfiguration(String id) {
        delete(configMapper, id);
    }

    @Override
    public List<DomainRecord> listSocialProviders() {
        return records(socialProviderMapper.selectByExample(new AuthSocialProviderDOExample()));
    }

    @Override
    public DomainRecord createSocialProvider(Map<String, Object> command) {
        return create(socialProviderMapper, new AuthSocialProviderDO(), command);
    }

    @Override
    public DomainRecord updateSocialProvider(String id, Map<String, Object> command) {
        return update(socialProviderMapper, new AuthSocialProviderDO(), id, command);
    }

    @Override
    public void deleteSocialProvider(String id) {
        delete(socialProviderMapper, id);
    }

    @Override
    public List<DomainRecord> listLoginLogs() {
        SysLoginLogDOExample example = new SysLoginLogDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(loginLogMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listTenants() {
        return records(tenantMapper.selectByExample(new SysTenantDOExample()));
    }

    @Override
    public List<DomainRecord> listPosts() {
        return records(postMapper.selectByExample(new SysPostDOExample()));
    }

    @Override
    public List<DomainRecord> listDictTypes() {
        return records(dictTypeMapper.selectByExample(new SysDictTypeDOExample()));
    }

    @Override
    public List<DomainRecord> listDictData() {
        return records(dictItemMapper.selectByExample(new SysDictItemDOExample()));
    }

    @Override
    public List<DomainRecord> listNotices() {
        return records(noticeMapper.selectByExample(new SysNoticeDOExample()));
    }

    @Override
    public List<DomainRecord> listOperationLogs() {
        SysOperLogDOExample example = new SysOperLogDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(operLogMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listOnlineSessions() {
        return records(sessionMapper.selectByExample(new AuthSessionDOExample()));
    }

    @Override
    public List<DomainRecord> listOauthClients() {
        return records(oauthClientMapper.selectByExample(new AuthOauthClientDOExample()));
    }

    @Override
    public void recordLogin(String username, String status, String message) {
        SysLoginLogDO row = new SysLoginLogDO();
        set(row, "tenantId", tenantId());
        set(row, "username", username);
        set(row, "loginName", username);
        set(row, "resultStatus", status);
        set(row, "loginStatus", status);
        set(row, "message", message);
        set(row, "loginMessage", message);
        touch(row, true);
        loginLogMapper.insertSelective(row);
    }

    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andStatusEqualTo("ENABLED");
        return records(socialProviderMapper.selectByExample(example));
    }

    @Override
    public Optional<DomainRecord> findSocialProvider(String provider) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andProviderCodeEqualTo(provider);
        return first(socialProviderMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public Optional<DomainRecord> findSocialBind(String provider, String openId) {
        Optional<DomainRecord> providerRecord = findSocialProvider(provider);
        Long providerId = providerRecord.map(record -> longValue(String.valueOf(record.get("id")))).orElse(null);
        if (providerId == null) {
            return Optional.empty();
        }
        AuthSocialBindingDOExample example = new AuthSocialBindingDOExample();
        example.createCriteria().andProviderIdEqualTo(providerId).andExternalUserIdEqualTo(openId);
        return first(socialBindingMapper.selectByExample(example)).map(this::record);
    }

    @Override
    public DomainRecord saveSocialBind(Map<String, Object> command) {
        return create(socialBindingMapper, new AuthSocialBindingDO(), command);
    }

    @Override
    public DomainRecord createSocialUser(Map<String, Object> command) {
        return createUser(command);
    }

    private Set<Long> roleIds(String userId) {
        Long id = longValue(userId);
        if (id == null) {
            return Collections.emptySet();
        }
        SysUserRoleDOExample example = new SysUserRoleDOExample();
        example.createCriteria().andUserIdEqualTo(id);
        return userRoleMapper.selectByExample(example)
                .stream()
                .map(SysUserRoleDOKey::getRoleId)
                .filter(value -> value != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> permissionIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        example.createCriteria().andRoleIdIn(new ArrayList<>(roleIds));
        return rolePermissionMapper.selectByExample(example)
                .stream()
                .map(SysRolePermissionDO::getPermissionId)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> menuIdsByPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        SysMenuPermissionDOExample example = new SysMenuPermissionDOExample();
        example.createCriteria().andPermissionIdIn(permissionIds);
        return menuPermissionMapper.selectByExample(example)
                .stream()
                .map(SysMenuPermissionDO::getMenuId)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private <T> Optional<T> first(List<T> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    private List<DomainRecord> records(List<?> rows) {
        return rows == null
                ? Collections.emptyList()
                : rows.stream().map(this::record).collect(Collectors.toList());
    }

    private DomainRecord record(Object row) {
        return DomainRecord.of(toMap(row));
    }

    private Map<String, Object> toMap(Object row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return values;
        }
        for (Method method : row.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getDeclaringClass() == Object.class) {
                continue;
            }
            String name = propertyName(method);
            if (name == null) {
                continue;
            }
            try {
                values.put(name, method.invoke(row));
            } catch (ReflectiveOperationException ignored) {
                // Keep dynamic records tolerant to generated model drift.
            }
        }
        return values;
    }

    private String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    private DomainRecord create(Object mapper, Object row, Map<String, Object> command) {
        applyCommand(row, command);
        touch(row, true);
        invoke(mapper, "insertSelective", row);
        return record(row);
    }

    private DomainRecord update(Object mapper, Object row, String id, Map<String, Object> command) {
        set(row, "id", longValue(id));
        applyCommand(row, command);
        touch(row, false);
        invoke(mapper, "updateByPrimaryKeySelective", row);
        Object persisted = invoke(mapper, "selectByPrimaryKey", longValue(id));
        return record(persisted == null ? row : persisted);
    }

    private void delete(Object mapper, String id) {
        invoke(mapper, "deleteByPrimaryKey", longValue(id));
    }

    private void applyCommand(Object row, Map<String, Object> command) {
        set(row, "tenantId", tenantId());
        if (command == null) {
            return;
        }
        for (Method method : row.getClass().getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterTypes().length != 1) {
                continue;
            }
            String property = decapitalize(method.getName().substring(3));
            if ("id".equals(property) || "tenantId".equals(property)) {
                continue;
            }
            Object value = command.containsKey(property) ? command.get(property) : command.get(camelToSnake(property));
            if (value != null) {
                set(row, property, value);
            }
        }
    }

    private void touch(Object row, boolean creating) {
        Date now = new Date();
        if (creating) {
            setIfNull(row, "tenantId", tenantId());
            setIfNull(row, "createTime", now);
            setIfNull(row, "version", 0);
            setIfNull(row, "deleteMarker", 0L);
            setIfBlank(row, "status", "ENABLED");
        }
        set(row, "updateTime", now);
    }

    private void setIfNull(Object row, String property, Object value) {
        if (get(row, property) == null) {
            set(row, property, value);
        }
    }

    private void setIfBlank(Object row, String property, String value) {
        Object current = get(row, property);
        if (current == null || StringUtils.isBlank(String.valueOf(current))) {
            set(row, property, value);
        }
    }

    private Object get(Object row, String property) {
        try {
            Method method = row.getClass().getMethod("get" + capitalize(property));
            return method.invoke(row);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void set(Object row, String property, Object value) {
        String setter = "set" + capitalize(property);
        for (Method method : row.getClass().getMethods()) {
            if (!method.getName().equals(setter) || method.getParameterTypes().length != 1
                    || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            try {
                method.invoke(row, convert(value, method.getParameterTypes()[0]));
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                // Missing fields are acceptable because generated platform tables vary.
            }
            return;
        }
    }

    private Object invoke(Object target, String methodName, Object argument) {
        if (argument == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterTypes().length != 1) {
                continue;
            }
            try {
                return method.invoke(target, convert(argument, method.getParameterTypes()[0]));
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                // Try the next overload if present.
            }
        }
        return null;
    }

    private Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return longValue(String.valueOf(value));
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(String.valueOf(value));
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Date.class && value instanceof Date) {
            return value;
        }
        return value;
    }

    private Long longValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String decapitalize(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private String camelToSnake(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
