package top.kx.heartbeat.infrastructure.platform;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.platform.*;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDeptDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDeptDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysJobDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysJobDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysMenuDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysMenuDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysMenuPermissionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysMenuPermissionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysPermissionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysPermissionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRolePermissionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRolePermissionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysTenantDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysTenantDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysTenantPlanDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysTenantPlanDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserRoleDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserRoleDOKey;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthOauthClientDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialProviderDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthOauthClientDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysConfigDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDeptDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysJobDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysMenuPermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysPermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRoleDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRolePermissionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysTenantDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysTenantPlanDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserRoleDOMapper;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * ??????????
 * <p>
 * ??????????????
 * <ul>
 *     <li>?????Enterprise Edition??/li>
 *     <li>?????heartbeat??????/li>
 *     <li>????????admin/admin123??/li>
 *     <li>?????super_admin????/????</li>
 *     <li>?????MOCK ?????????Quartz ??</li>
 * </ul>
 * ?? {@code heartbeat.bootstrap.platform-seed-enabled} ??????
 * </p>
 *
 * @author heartbeat-team
 */
@Component
@ConditionalOnProperty(
        prefix = "heartbeat.bootstrap",
        name = "platform-seed-enabled",
        havingValue = "true"
)
public class PlatformSeedInitializer {

    private static final long DEFAULT_TENANT_ID = 1L;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SysTenantPlanDOMapper tenantPlanMapper;
    @Resource
    private SysTenantDOMapper tenantMapper;
    @Resource
    private SysDeptDOMapper deptMapper;
    @Resource
    private SysUserDOMapper userMapper;
    @Resource
    private SysRoleDOMapper roleMapper;
    @Resource
    private SysUserRoleDOMapper userRoleMapper;
    @Resource
    private SysMenuDOMapper menuMapper;
    @Resource
    private SysPermissionDOMapper permissionMapper;
    @Resource
    private SysMenuPermissionDOMapper menuPermissionMapper;
    @Resource
    private SysRolePermissionDOMapper rolePermissionMapper;
    @Resource
    private SysConfigDOMapper configMapper;
    @Resource
    private AuthSocialProviderDOMapper socialProviderMapper;
    @Resource
    private SysJobDOMapper jobMapper;
    @Resource
    private SecretCryptoService secretCryptoService;

    /**
     * ??????????????????????????
     */
    @PostConstruct
    public void initialize() {
        TenantContext.runAsPlatform(() -> {
            initializeInternal();
            return null;
        });
    }

    /**
     * ??????????/??/??/??/??/??/??/????
     */
    private void initializeInternal() {
        SysTenantPlanDO plan = seedPlan();
        SysTenantDO tenant = seedTenant(plan.getId());
        SysDeptDO dept = seedDept(tenant.getId());
        SysUserDO admin = seedAdminUser(tenant.getId(), dept.getId());
        SysRoleDO role = seedSuperAdminRole(tenant.getId());
        seedUserRole(tenant.getId(), admin.getId(), role.getId());
        seedMenusAndPermissions(tenant.getId(), role.getId());
        seedSystemConfig(tenant.getId());
        seedMockSocialProvider(tenant.getId());
        seedDemoJob(tenant.getId());
    }

    /**
     * ????????????????????
     *
     * @return ??????????
     */
    private SysTenantPlanDO seedPlan() {
        SysTenantPlanDO existing = findPlan(true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysTenantPlanDO plan = new SysTenantPlanDO();
        plan.setPlanCode(PlatformTenantType.ENTERPRISE.getCode());
        plan.setPlanName("Enterprise Edition");
        plan.setPlanType(PlatformTenantType.ENTERPRISE.getCode());
        plan.setDescription("Default enterprise plan");
        plan.setMaxUserCount(1000);
        plan.setMaxStorageMb(102400L);
        plan.setFeaturePolicy("{\"edition\":\"enterprise\"}");
        plan.setStatus(PlatformRecordStatus.ENABLED.getCode());
        plan.setSortNo(1);
        plan.setVersion(0);
        plan.setDeleteMarker(0L);
        plan.setCreateTime(toDate(now));
        plan.setUpdateTime(toDate(now));
        tenantPlanMapper.insertSelective(plan);
        return plan;
    }

    /**
     * ?????????code=heartbeat???????????
     *
     * @param planId ?? ID
     * @return ??????????
     */
    private SysTenantDO seedTenant(Long planId) {
        SysTenantDO existing = findTenant(true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysTenantDO tenant = new SysTenantDO();
        tenant.setPlanId(planId);
        tenant.setTenantCode("heartbeat");
        tenant.setTenantName("HeartBeat Platform");
        tenant.setTenantType(PlatformTenantType.ENTERPRISE.getCode());
        tenant.setTimezone("Asia/Shanghai");
        tenant.setLocale("zh-CN");
        tenant.setStatus(PlatformRecordStatus.ENABLED.getCode());
        tenant.setVersion(0);
        tenant.setDeleteMarker(0L);
        tenant.setCreateTime(toDate(now));
        tenant.setUpdateTime(toDate(now));
        tenantMapper.insertSelective(tenant);
        return tenant;
    }

    /**
     * ????????deptCode=platform???????????
     *
     * @param tenantId ?? ID
     * @return ??????????
     */
    private SysDeptDO seedDept(Long tenantId) {
        SysDeptDO existing = findDept(tenantId, true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysDeptDO dept = new SysDeptDO();
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setDeptCode("platform");
        dept.setDeptName("Platform Root Department");
        dept.setAncestors("0");
        dept.setDeptLevel(1);
        dept.setSortNo(1);
        dept.setStatus(PlatformRecordStatus.ENABLED.getCode());
        dept.setVersion(0);
        dept.setDeleteMarker(0L);
        dept.setCreateTime(toDate(now));
        dept.setUpdateTime(toDate(now));
        deptMapper.insertSelective(dept);
        return dept;
    }

    /**
     * ??????????admin/admin123???????????
     *
     * @param tenantId ?? ID
     * @param deptId   ?? ID
     * @return ??????????
     */
    private SysUserDO seedAdminUser(Long tenantId, Long deptId) {
        SysUserDO existing = findUser(tenantId, true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysUserDO user = new SysUserDO();
        user.setTenantId(tenantId);
        user.setDeptId(deptId);
        user.setUsername("admin");
        user.setNickname("Administrator");
        user.setRealName("Platform Administrator");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setPasswordAlgo("BCRYPT");
        user.setPasswordUpdateTime(toDate(now));
        user.setUserType("SUPER_ADMIN");
        user.setStatus(PlatformRecordStatus.ENABLED.getCode());
        user.setVersion(0);
        user.setDeleteMarker(0L);
        user.setCreateTime(toDate(now));
        user.setUpdateTime(toDate(now));
        userMapper.insertSelective(user);
        return user;
    }

    /**
     * ???????roleCode=super_admin???????????
     *
     * @param tenantId ?? ID
     * @return ??????????
     */
    private SysRoleDO seedSuperAdminRole(Long tenantId) {
        SysRoleDO existing = findRole(tenantId, true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysRoleDO role = new SysRoleDO();
        role.setTenantId(tenantId);
        role.setRoleCode("super_admin");
        role.setRoleName("Super Administrator");
        role.setRoleType("SYSTEM");
        role.setDataScope("ALL");
        role.setDescription("Built-in platform super administrator role");
        role.setSortNo(1);
        role.setStatus(PlatformRecordStatus.ENABLED.getCode());
        role.setVersion(0);
        role.setDeleteMarker(0L);
        role.setCreateTime(toDate(now));
        role.setUpdateTime(toDate(now));
        roleMapper.insertSelective(role);
        return role;
    }

    /**
     * ??????????????????
     */
    private void seedUserRole(Long tenantId, Long userId, Long roleId) {
        if (hasUserRole(tenantId, userId, roleId)) {
            return;
        }
        SysUserRoleDOKey relation = new SysUserRoleDOKey();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insertSelective(relation);
    }

    /**
     * ?? Dashboard / System ??????????????????-?????????????
     */
    private void seedMenusAndPermissions(Long tenantId, Long roleId) {
        seedMenu(tenantId, 0L, "dashboard", "Dashboard", PlatformMenuType.MENU.getCode(),
                "/dashboard", "dashboard/index", "dashboard", 1);
        seedMenu(tenantId, 0L, "root:system", "System", PlatformMenuType.CATALOG.getCode(),
                "/system", null, "system", 10);
        SysMenuDO systemUser = seedMenu(tenantId, menuId(tenantId, "root:system"), "system:user", "Users", PlatformMenuType.MENU.getCode(),
                "/system/user", "system/user/index", "user", 12);
        SysMenuDO systemRole = seedMenu(tenantId, menuId(tenantId, "root:system"), "system:role", "Roles", PlatformMenuType.MENU.getCode(),
                "/system/role", "system/role/index", "role", 15);
        SysMenuDO systemMenu = seedMenu(tenantId, menuId(tenantId, "root:system"), "system:menu", "Menus", PlatformMenuType.MENU.getCode(),
                "/system/menu", "system/menu/index", "menu", 16);

        List<SysPermissionDO> permissions = Arrays.asList(
                seedPermission(tenantId, "dashboard:view", "Dashboard View", "/api/v1/admin/modules", "GET", 1),
                seedPermission(tenantId, "system:user:list", "User List", "/api/v1/admin/resources/users", "GET", 20),
                seedPermission(tenantId, "system:user:add", "User Add", "/api/v1/admin/resources/users", "POST", 21),
                seedPermission(tenantId, "system:user:edit", "User Edit", "/api/v1/admin/resources/users/{id}", "PUT", 22),
                seedPermission(tenantId, "system:user:remove", "User Remove", "/api/v1/admin/resources/users/{id}", "DELETE", 23),
                seedPermission(tenantId, "system:role:list", "Role List", "/api/v1/admin/resources/roles", "GET", 50),
                seedPermission(tenantId, "system:role:grant", "Role Grant", "/api/v1/iam/roles/{id}/menus", "PUT", 54),
                seedPermission(tenantId, "system:menu:list", "Menu List", "/api/v1/iam/menus", "GET", 60),
                seedPermission(tenantId, "system:menu:add", "Menu Add", "/api/v1/iam/menus", "POST", 61),
                seedPermission(tenantId, "system:menu:edit", "Menu Edit", "/api/v1/iam/menus/{id}", "PUT", 62),
                seedPermission(tenantId, "system:menu:remove", "Menu Remove", "/api/v1/iam/menus/{id}", "DELETE", 63)
        );
        linkMenuPermission(menuId(tenantId, "dashboard"), permissionId(tenantId, "dashboard:view"), tenantId);
        for (SysPermissionDO permission : permissions) {
            if (permission.getPermissionCode().startsWith("system:user:")) {
                linkMenuPermission(systemUser.getId(), permission.getId(), tenantId);
            }
            if (permission.getPermissionCode().startsWith("system:role:")) {
                linkMenuPermission(systemRole.getId(), permission.getId(), tenantId);
            }
            if (permission.getPermissionCode().startsWith("system:menu:")) {
                linkMenuPermission(systemMenu.getId(), permission.getId(), tenantId);
            }
            linkRolePermission(roleId, permission.getId(), tenantId);
        }
    }

    /**
     * ??/?????????????????
     */
    private SysMenuDO seedMenu(Long tenantId, Long parentId, String code, String name, String type,
                                   String routePath, String componentPath, String icon, int sortNo) {
        SysMenuDO existing = findMenu(tenantId, code, true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysMenuDO menu = new SysMenuDO();
        menu.setTenantId(tenantId);
        menu.setParentId(parentId);
        menu.setMenuCode(code);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setRoutePath(routePath);
        menu.setComponentPath(componentPath);
        menu.setIcon(icon);
        menu.setVisible(true);
        menu.setKeepAlive(false);
        menu.setPermissionMode(PlatformPermissionMode.RELATION.getCode());
        menu.setSortNo(sortNo);
        menu.setStatus(PlatformRecordStatus.ENABLED.getCode());
        menu.setVersion(0);
        menu.setDeleteMarker(0L);
        menu.setCreateTime(toDate(now));
        menu.setUpdateTime(toDate(now));
        menuMapper.insertSelective(menu);
        return menu;
    }

    /**
     * ??/?????API ??????????????
     */
    private SysPermissionDO seedPermission(
            Long tenantId,
            String code,
            String name,
            String path,
            String method,
            int sortNo
    ) {
        SysPermissionDO existing = findPermission(tenantId, code, true);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = now();
        SysPermissionDO permission = new SysPermissionDO();
        permission.setTenantId(tenantId);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setPermissionType(PlatformPermissionType.API.getCode());
        permission.setResourceType(PlatformResourceType.HTTP_API.getCode());
        permission.setResourcePath(path);
        permission.setHttpMethod(method);
        permission.setDescription(name);
        permission.setStatus(PlatformRecordStatus.ENABLED.getCode());
        permission.setSortNo(sortNo);
        permission.setVersion(0);
        permission.setDeleteMarker(0L);
        permission.setCreateTime(toDate(now));
        permission.setUpdateTime(toDate(now));
        permissionMapper.insertSelective(permission);
        return permission;
    }

    /**
     * ????????????????????
     */
    private void linkMenuPermission(Long menuId, Long permissionId, Long tenantId) {
        if (menuId == null || permissionId == null) {
            return;
        }
        if (hasMenuPermission(tenantId, menuId, permissionId)) {
            return;
        }
        SysMenuPermissionDO relation = new SysMenuPermissionDO();
        relation.setTenantId(tenantId);
        relation.setMenuId(menuId);
        relation.setPermissionId(permissionId);
        relation.setCreateTime(new Date());
        menuPermissionMapper.insertSelective(relation);
    }

    /**
     * ????????????????????
     */
    private void linkRolePermission(Long roleId, Long permissionId, Long tenantId) {
        if (hasRolePermission(tenantId, roleId, permissionId)) {
            return;
        }
        SysRolePermissionDO relation = new SysRolePermissionDO();
        relation.setTenantId(tenantId);
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        relation.setCreateTime(new Date());
        rolePermissionMapper.insertSelective(relation);
    }

    /**
     * ?????????? system.name=HeartBeat???
     */
    private void seedSystemConfig(Long tenantId) {
        if (hasConfig(tenantId, "system.name")) {
            return;
        }
        LocalDateTime now = now();
        SysConfigDO config = new SysConfigDO();
        config.setTenantId(tenantId);
        config.setConfigKey("system.name");
        config.setConfigName("System Name");
        config.setConfigValue("HeartBeat");
        config.setValueType("STRING");
        config.setEncrypted(false);
        config.setConfigGroup("system");
        config.setDescription("Default system display name");
        config.setStatus(PlatformRecordStatus.ENABLED.getCode());
        config.setVersion(0);
        config.setDeleteMarker(0L);
        config.setCreateTime(toDate(now));
        config.setUpdateTime(toDate(now));
        configMapper.insertSelective(config);
    }

    /**
     * ???????MOCK ????????????????
     */
    private void seedMockSocialProvider(Long tenantId) {
        if (hasSocialProvider(tenantId, "MOCK")) {
            return;
        }
        LocalDateTime now = now();
        AuthSocialProviderDO provider = new AuthSocialProviderDO();
        provider.setTenantId(tenantId);
        provider.setProviderCode("MOCK");
        provider.setProviderName("Mock Login");
        provider.setProviderType("MOCK");
        provider.setClientId("mock-app");
        provider.setAppSecretCipher(secretCryptoService.encryptIfPlain("mock-secret"));
        provider.setAuthorizeUrl("");
        provider.setTokenUrl("");
        provider.setUserInfoUrl("");
        provider.setScopes("");
        provider.setEnabled(true);
        provider.setStatus(PlatformRecordStatus.ENABLED.getCode());
        provider.setVersion(0);
        provider.setDeleteMarker(0L);
        provider.setCreateTime(toDate(now));
        provider.setUpdateTime(toDate(now));
        socialProviderMapper.insertSelective(provider);
    }

    /**
     * ???? Quartz ???job-demo????????????
     */
    private void seedDemoJob(Long tenantId) {
        if (hasJob(tenantId, "job-demo")) {
            return;
        }
        LocalDateTime now = now();
        SysJobDO job = new SysJobDO();
        job.setTenantId(tenantId);
        job.setJobCode("job-demo");
        job.setJobName("Demo Quartz Job");
        job.setJobGroup("DEFAULT");
        job.setInvokeTarget("demoQuartzJob.run");
        job.setCronExpression("0 0 0 1 1 ? 2099");
        job.setMisfirePolicy("SMART");
        job.setConcurrent(false);
        job.setStatus(PlatformRecordStatus.ENABLED.getCode());
        job.setVersion(0);
        job.setDeleteMarker(0L);
        job.setCreateTime(toDate(now));
        job.setUpdateTime(toDate(now));
        jobMapper.insertSelective(job);
    }

    /**
     * ????????????ID????????????
     */
    private Long menuId(Long tenantId, String menuCode) {
        SysMenuDO menu = findMenu(tenantId, menuCode, true);
        return menu == null ? null : menu.getId();
    }

    /**
     * ????????????ID??
     */
    private Long permissionId(Long tenantId, String permissionCode) {
        SysPermissionDO permission = findPermission(tenantId, permissionCode, true);
        return permission == null ? null : permission.getId();
    }

    /**
     * ???????????????????
     */
    private SysTenantPlanDO findPlan(boolean excludeDeleted) {
        SysTenantPlanDOExample example = new SysTenantPlanDOExample();
        SysTenantPlanDOExample.Criteria criteria = example.createCriteria()
                .andPlanCodeEqualTo(PlatformTenantType.ENTERPRISE.getCode());
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(tenantPlanMapper.selectByExample(example));
    }

    private SysTenantDO findTenant(boolean excludeDeleted) {
        SysTenantDOExample example = new SysTenantDOExample();
        SysTenantDOExample.Criteria criteria = example.createCriteria()
                .andTenantCodeEqualTo("heartbeat");
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(tenantMapper.selectByExample(example));
    }

    private SysDeptDO findDept(Long tenantId, boolean excludeDeleted) {
        SysDeptDOExample example = new SysDeptDOExample();
        SysDeptDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andDeptCodeEqualTo("platform");
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(deptMapper.selectByExample(example));
    }

    private SysUserDO findUser(Long tenantId, boolean excludeDeleted) {
        SysUserDOExample example = new SysUserDOExample();
        SysUserDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andUsernameEqualTo("admin");
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(userMapper.selectByExample(example));
    }

    private SysRoleDO findRole(Long tenantId, boolean excludeDeleted) {
        SysRoleDOExample example = new SysRoleDOExample();
        SysRoleDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andRoleCodeEqualTo("super_admin");
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(roleMapper.selectByExample(example));
    }

    private SysMenuDO findMenu(Long tenantId, String code, boolean excludeDeleted) {
        SysMenuDOExample example = new SysMenuDOExample();
        SysMenuDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andMenuCodeEqualTo(code);
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(menuMapper.selectByExample(example));
    }

    private SysPermissionDO findPermission(Long tenantId, String code, boolean excludeDeleted) {
        SysPermissionDOExample example = new SysPermissionDOExample();
        SysPermissionDOExample.Criteria criteria = example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andPermissionCodeEqualTo(code);
        if (excludeDeleted) {
            criteria.andDeleteMarkerEqualTo(0L);
        }
        return first(permissionMapper.selectByExample(example));
    }

    private boolean hasUserRole(Long tenantId, Long userId, Long roleId) {
        SysUserRoleDOExample example = new SysUserRoleDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andUserIdEqualTo(userId)
                .andRoleIdEqualTo(roleId);
        return userRoleMapper.countByExample(example) > 0;
    }

    private boolean hasMenuPermission(Long tenantId, Long menuId, Long permissionId) {
        SysMenuPermissionDOExample example = new SysMenuPermissionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andMenuIdEqualTo(menuId)
                .andPermissionIdEqualTo(permissionId);
        return menuPermissionMapper.countByExample(example) > 0;
    }

    private boolean hasRolePermission(Long tenantId, Long roleId, Long permissionId) {
        SysRolePermissionDOExample example = new SysRolePermissionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andRoleIdEqualTo(roleId)
                .andPermissionIdEqualTo(permissionId);
        return rolePermissionMapper.countByExample(example) > 0;
    }

    private boolean hasConfig(Long tenantId, String configKey) {
        SysConfigDOExample example = new SysConfigDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andConfigKeyEqualTo(configKey)
                .andDeleteMarkerEqualTo(0L);
        return configMapper.countByExample(example) > 0;
    }

    private boolean hasSocialProvider(Long tenantId, String providerCode) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andProviderCodeEqualTo(providerCode)
                .andDeleteMarkerEqualTo(0L);
        return socialProviderMapper.countByExample(example) > 0;
    }

    private boolean hasJob(Long tenantId, String jobCode) {
        SysJobDOExample example = new SysJobDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andJobCodeEqualTo(jobCode)
                .andDeleteMarkerEqualTo(0L);
        return jobMapper.countByExample(example) > 0;
    }

    private <T> T first(List<T> records) {
        return records.isEmpty() ? null : records.get(0);
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private static Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
}
