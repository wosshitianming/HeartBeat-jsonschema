package top.kx.heartbeat.application.platform;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.auth.AuthenticationSessionService;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.port.*;
import top.kx.heartbeat.application.platform.request.*;
import top.kx.heartbeat.application.platform.response.LoginResponse;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.auth.LoginResultStatus;
import top.kx.heartbeat.domain.platform.PlatformMenuType;
import top.kx.heartbeat.domain.security.DataScope;
import top.kx.heartbeat.domain.user.model.valueobject.UserStatus;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;


/**
 * 编排平台管理应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class PlatformAdministrationService {

    private static final String DEFAULT_ADMIN_USER_ID = "1";

    private static final String LEGACY_APPEARANCE_THEME_KEY = "appearance.theme";

    private static final String APPEARANCE_COLOR_MODE_KEY = "appearance.colorMode";

    private static final String APPEARANCE_FLUID_ENABLED_KEY = "appearance.fluidEnabled";

    private static final String APPEARANCE_ACCENT_COLOR_KEY = "appearance.accentColor";

    private static final String APPEARANCE_VISUAL_STYLE_KEY = "appearance.visualStyle";

    private static final AppearanceColorMode DEFAULT_COLOR_MODE = AppearanceColorMode.DARK;

    private static final boolean DEFAULT_FLUID_ENABLED = true;

    private static final String DEFAULT_ACCENT_COLOR = "#1677ff";

    private static final AppearanceVisualStyle DEFAULT_VISUAL_STYLE = AppearanceVisualStyle.GLASS;

    private static final String LEGACY_PROFESSIONAL_THEME = "professional";

    private static final Pattern ACCENT_COLOR_PATTERN =
            Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    @Resource
    private PlatformUserRepository platformUserRepository;
    @Resource
    private PlatformPermissionRepository platformPermissionRepository;
    @Resource
    private PlatformMenuRepository platformMenuRepository;
    @Resource
    private PlatformOrganizationRepository platformOrganizationRepository;
    @Resource
    private PlatformRoleRepository platformRoleRepository;
    @Resource
    private PlatformConfigRepository platformConfigRepository;
    @Resource
    private PlatformSocialRepository platformSocialRepository;
    @Resource
    private PlatformAuditQueryRepository platformAuditQueryRepository;
    @Resource
    private PlatformNoticeRepository platformNoticeRepository;

    @Resource
    private PlatformLoginLogRepository platformLoginLogRepository;

    @Resource
    private AuthenticationSessionService authenticationSessionService;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public RecordResponse currentUser() {
        // 使用当前登录用户标识查询用户资料。
        return currentUser(currentUserProvider.currentUserId());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse currentUser(String userId) {
        // 解析用户标识，空值时回落到默认管理员。
        String resolvedUserId = resolvedUserId(userId);
        // 查询用户行数据，未查到时使用内置管理员兜底。
        Map<String, Object> user = platformUserRepository.findUserById(resolvedUserId)
                .map(DomainRecord::toMap)
                .orElseGet(this::fallbackUser);
        // 移除敏感字段后构建接口返回结构。
        Map<String, Object> result = publicUser(user);
        // 附加当前用户角色编码列表。
        result.put("roles", platformPermissionRepository.listRoleCodesByUserId(resolvedUserId));
        // 附加当前用户权限编码列表。
        result.put("permissions", platformPermissionRepository.listPermissionsByUserId(String.valueOf(user.get("id"))));
        // 附加当前用户数据权限范围。
        result.put("dataScope", resolveDataScope(resolvedUserId));
        // 返回当前用户资料。
        return RecordResponse.from(result);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public RecordResponse appearancePreference() {
        // 使用当前登录用户标识查询外观偏好。
        return appearancePreference(currentUserProvider.currentUserId());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse appearancePreference(String userId) {
        // 解析用户标识，空值时回落到默认管理员。
        String resolvedUserId = resolvedUserId(userId);
        // 查询颜色模式偏好。
        Optional<Map<String, Object>> colorPreference =
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_COLOR_MODE_KEY));
        // 查询流体布局偏好。
        Optional<Map<String, Object>> fluidPreference =
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_FLUID_ENABLED_KEY));
        // 查询强调色偏好。
        Optional<Map<String, Object>> accentPreference =
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_ACCENT_COLOR_KEY));
        // 查询视觉风格偏好。
        Optional<Map<String, Object>> visualPreference =
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_VISUAL_STYLE_KEY));

        // 新版偏好任意一项存在时按新版结构返回。
        if (colorPreference.isPresent() || fluidPreference.isPresent() || accentPreference.isPresent()
                || visualPreference.isPresent()) {
            // 解析颜色模式，缺省时使用默认颜色模式。
            String colorMode = colorPreference
                    .map(preference -> stringValue(preference.get("preferenceValue")))
                    .orElse(DEFAULT_COLOR_MODE.getCode());
            // 解析流体布局开关，缺省时使用默认流体布局开关。
            boolean fluidEnabled = fluidPreference
                    .map(preference -> Boolean.parseBoolean(stringValue(preference.get("preferenceValue"))))
                    .orElse(DEFAULT_FLUID_ENABLED);
            // 解析强调色，缺省时使用默认强调色。
            String accentColor = accentPreference
                    .map(preference -> normalizeAccentColor(stringValue(preference.get("preferenceValue"))))
                    .orElse(DEFAULT_ACCENT_COLOR);
            // 解析视觉风格，缺省时使用默认视觉风格。
            String visualStyle = visualPreference
                    .map(preference -> normalizeVisualStyle(stringValue(preference.get("preferenceValue"))))
                    .orElse(DEFAULT_VISUAL_STYLE.getCode());
            // 返回新版外观偏好结构。
            return RecordResponse.from(appearanceResult(colorMode, fluidEnabled, accentColor, visualStyle));
        }

        // 新版偏好不存在时兼容读取旧版主题偏好。
        return platformUserRepository.findUserPreference(resolvedUserId, LEGACY_APPEARANCE_THEME_KEY)
                .map(DomainRecord::toMap)
                .map(this::legacyAppearanceResult)
                .map(RecordResponse::from)
                .orElseGet(() -> RecordResponse.from(appearanceResult(
                        DEFAULT_COLOR_MODE.getCode(),
                        DEFAULT_FLUID_ENABLED,
                        DEFAULT_ACCENT_COLOR,
                        DEFAULT_VISUAL_STYLE.getCode()
                )));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateAppearancePreference(PlatformAppearancePreferenceRequest request) {
        // 使用当前登录用户标识更新外观偏好。
        return updateAppearancePreference(currentUserProvider.currentUserId(), request);
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateAppearancePreference(String userId, PlatformAppearancePreferenceRequest request) {
        PlatformAppearancePreferenceRequest safeRequest =
                request == null ? new PlatformAppearancePreferenceRequest() : request;
        // 查询当前外观偏好作为局部更新的默认值来源。
        Map<String, Object> current = appearancePreference(userId).toMap();
        // 解析颜色模式入参，未传时沿用当前值。
        String colorMode = safeRequest.getColorMode() != null
                ? stringValue(safeRequest.getColorMode())
                : stringValue(current.get("colorMode"));
        // 解析流体布局入参，未传时沿用当前值。
        boolean fluidEnabled = safeRequest.getFluidEnabled() != null
                ? safeRequest.getFluidEnabled()
                : Boolean.TRUE.equals(current.get("fluidEnabled"));
        // 解析强调色入参，未传时沿用当前值。
        String accentColor = safeRequest.getAccentColor() != null
                ? normalizeAccentColor(stringValue(safeRequest.getAccentColor()))
                : stringValue(current.get("accentColor"));
        // 解析视觉风格入参，未传时沿用当前值。
        String visualStyle = safeRequest.getVisualStyle() != null
                ? normalizeVisualStyle(stringValue(safeRequest.getVisualStyle()))
                : stringValue(current.get("visualStyle"));

        // 固化原始颜色模式入参，供异常消息安全引用。
        String requestedColorMode = colorMode;
        // 使用颜色模式枚举校验并规范化颜色模式编码。
        colorMode = AppearanceColorMode.fromCode(requestedColorMode)
                .orElseThrow(() -> new IllegalArgumentException("不支持的颜色模式: " + requestedColorMode))
                .getCode();

        // 解析用户标识，空值时回落到默认管理员。
        String resolvedUserId = resolvedUserId(userId);
        // 保存颜色模式偏好。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_COLOR_MODE_KEY, colorMode);
        // 保存流体布局偏好。
        platformUserRepository.saveUserPreference(
                resolvedUserId,
                APPEARANCE_FLUID_ENABLED_KEY,
                String.valueOf(fluidEnabled)
        );
        // 保存强调色偏好。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_ACCENT_COLOR_KEY, accentColor);
        // 保存视觉风格偏好。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_VISUAL_STYLE_KEY, visualStyle);
        // 返回更新后的外观偏好。
        return RecordResponse.from(appearanceResult(colorMode, fluidEnabled, accentColor, visualStyle));
    }

    /**
     * 完成登录校验并组装前端需要的登录态信息，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public LoginResponse login(PlatformLoginRequest request) {
        PlatformLoginRequest safeRequest = request == null ? new PlatformLoginRequest() : request;
        // 读取登录用户名。
        String username = stringValue(safeRequest.getUsername());
        // 读取登录密码。
        String password = stringValue(safeRequest.getPassword());
        // 按用户名查询用户记录。
        Optional<Map<String, Object>> userOptional = optionalMap(platformUserRepository.findUserByUsername(username));
        // 用户不存在或密码不匹配时记录失败日志并中断登录。
        if (!userOptional.isPresent() || !matches(password, stringValue(userOptional.get().get("passwordHash")))) {
            // 写入失败登录日志。
            platformLoginLogRepository.recordLogin(username, LoginResultStatus.FAIL.getCode(), "用户名或密码错误");
            // 抛出用户名或密码错误异常。
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 写入成功登录日志。
        platformLoginLogRepository.recordLogin(username, LoginResultStatus.SUCCESS.getCode(), "账号密码登录成功");
        // 过滤用户敏感字段。
        Map<String, Object> user = publicUser(userOptional.get());
        // 创建认证会话并复制为可扩展返回结构。
        AuthTokenResponse tokens = authenticationSessionService.createSession(
                String.valueOf(user.get("id")),
                String.valueOf(user.get("username")),
                String.valueOf(user.get("tenantId")));
        // 返回登录结果。
        return LoginResponse.of(
                tokens,
                RecordResponse.from(user),
                platformPermissionRepository.listPermissionsByUserId(String.valueOf(user.get("id")))
        );
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listMenus() {
        // 查询菜单记录并构建包含按钮节点的树结构。
        return RecordResponse.fromMaps(buildTree(maps(platformMenuRepository.listMenus()), true));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> routes() {
        // 使用当前登录用户标识查询路由树。
        return routes(currentUserProvider.currentUserId());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> routes(String userId) {
        // 查询授权菜单记录并构建不包含按钮节点的路由树。
        return RecordResponse.fromMaps(buildTree(maps(platformMenuRepository.listAuthorizedMenus(resolvedUserId(userId))), false));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> menuTreeSelect() {
        // 构建完整菜单树。
        List<Map<String, Object>> tree = buildTree(maps(platformMenuRepository.listMenus()), true);
        // 将菜单树转换为选择器节点结构。
        return RecordResponse.fromMaps(toTreeSelect(tree));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createMenu(PlatformMenuRequest request) {
        // 委托仓储创建菜单并返回字段 Map。
        return RecordResponse.from(platformMenuRepository.createMenu(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateMenu(String id, PlatformMenuRequest request) {
        // 委托仓储更新菜单并返回字段 Map。
        return RecordResponse.from(platformMenuRepository.updateMenu(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteMenu(String id) {
        // 委托仓储删除菜单。
        platformMenuRepository.deleteMenu(id);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param roleId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse roleMenuDetail(String roleId) {
        // 角色不存在时抛出业务参数异常。
        if (!platformPermissionRepository.roleExists(roleId)) {
            // 抛出角色不存在异常。
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }
        // 创建角色菜单详情返回结构。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入角色标识。
        result.put("roleId", roleId);
        // 写入角色已授权菜单标识列表。
        result.put("menuIds", platformPermissionRepository.listRoleMenuIds(roleId));
        // 写入菜单树选择器数据。
        result.put("menuTree", menuTreeSelect());
        // 返回角色菜单授权详情。
        return RecordResponse.from(result);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param roleId 业务记录标识。
     * @param menuIds 业务处理所需参数。
     */
    @Transactional
    public void assignRoleMenus(String roleId, List<String> menuIds) {
        // 角色不存在时抛出业务参数异常。
        if (!platformPermissionRepository.roleExists(roleId)) {
            // 抛出角色不存在异常。
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }
        // 规范化菜单标识列表，去掉空值和重复值。
        List<String> normalized = menuIds == null
                ? Collections.emptyList()
                : menuIds.stream().map(this::stringValue).filter(id -> StringUtils.isNotEmpty(id)).distinct()
                .collect(java.util.stream.Collectors.toList());
        // 保存角色与菜单的授权关系。
        platformPermissionRepository.saveRoleMenus(roleId, normalized);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listUsers() {
        // 查询用户列表后按当前用户数据权限过滤并移除敏感字段。
        return RecordResponse.fromMaps(filterUsersByDataScope(maps(platformUserRepository.listUsers()), currentUserProvider.currentUserId()).stream()
                .map(this::publicUser)
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createUser(PlatformUserRequest request) {
        // 通过平台管理仓储创建用户记录。
        return RecordResponse.from(platformUserRepository.createUser(prepareUserRequest(request)));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateUser(String id, PlatformUserRequest request) {
        // 通过平台管理仓储更新用户记录。
        return RecordResponse.from(platformUserRepository.updateUser(id, prepareUserRequest(request)));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteUser(String id) {
        // 通过平台管理仓储删除用户记录。
        platformUserRepository.deleteUser(id);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDepartments() {
        // 通过平台管理仓储查询部门列表。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listDepartments()));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createDepartment(PlatformDepartmentRequest request) {
        // 通过平台管理仓储创建部门记录。
        return RecordResponse.from(platformOrganizationRepository.createDepartment(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateDepartment(String id, PlatformDepartmentRequest request) {
        // 通过平台管理仓储更新部门记录。
        return RecordResponse.from(platformOrganizationRepository.updateDepartment(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteDepartment(String id) {
        // 通过平台管理仓储删除部门记录。
        platformOrganizationRepository.deleteDepartment(id);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listRoles() {
        // 通过平台管理仓储查询角色列表。
        return RecordResponse.fromMaps(maps(platformRoleRepository.listRoles()));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createRole(PlatformRoleRequest request) {
        // 通过平台管理仓储创建角色记录。
        return RecordResponse.from(platformRoleRepository.createRole(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateRole(String id, PlatformRoleRequest request) {
        // 通过平台管理仓储更新角色记录。
        return RecordResponse.from(platformRoleRepository.updateRole(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteRole(String id) {
        // 通过平台管理仓储删除角色记录。
        platformRoleRepository.deleteRole(id);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listConfigurations() {
        // 通过平台管理仓储查询参数配置列表。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listConfigurations()));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createConfiguration(PlatformConfigurationRequest request) {
        // 通过平台管理仓储创建参数配置记录。
        return RecordResponse.from(platformConfigRepository.createConfiguration(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateConfiguration(String id, PlatformConfigurationRequest request) {
        // 通过平台管理仓储更新参数配置记录。
        return RecordResponse.from(platformConfigRepository.updateConfiguration(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteConfiguration(String id) {
        // 通过平台管理仓储删除参数配置记录。
        platformConfigRepository.deleteConfiguration(id);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listSocialProviders() {
        // 通过平台管理仓储查询社交登录渠道列表。
        return RecordResponse.fromMaps(maps(platformSocialRepository.listSocialProviders()));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse createSocialProvider(PlatformSocialProviderRequest request) {
        // 通过平台管理仓储创建社交登录渠道记录。
        return RecordResponse.from(platformSocialRepository.createSocialProvider(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse updateSocialProvider(String id, PlatformSocialProviderRequest request) {
        // 通过平台管理仓储更新社交登录渠道记录。
        return RecordResponse.from(platformSocialRepository.updateSocialProvider(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，协调平台管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     */
    @Transactional
    public void deleteSocialProvider(String id) {
        // 通过平台管理仓储删除社交登录渠道记录。
        platformSocialRepository.deleteSocialProvider(id);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listLoginLogs() {
        // 通过平台管理仓储查询登录日志列表。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listLoginLogs()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listTenants() {
        // 通过平台管理仓储查询租户列表。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listTenants()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listPosts() {
        // 通过平台管理仓储查询岗位列表。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listPosts()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDictTypes() {
        // 通过平台管理仓储查询字典类型列表。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listDictTypes()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listDictData() {
        // 通过平台管理仓储查询字典数据列表。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listDictData()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listNotices() {
        // 通过平台管理仓储查询通知公告列表。
        return RecordResponse.fromMaps(maps(platformNoticeRepository.listNotices()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listOperationLogs() {
        // 通过平台管理仓储查询操作日志列表。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOperationLogs()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listOnlineSessions() {
        // 通过平台管理仓储查询在线会话列表。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOnlineSessions()));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listOauthClients() {
        // 通过平台管理仓储查询 OAuth 客户端列表。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOauthClients()));
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调平台管理相关仓储和领域规则。
     *
     * @param menus 业务处理所需参数。
     * @param includeButtons 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> menus, boolean includeButtons) {
        // 创建菜单标识到菜单节点的有序索引。
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        // 遍历菜单平铺列表。
        for (Map<String, Object> menu : menus) {
            // 读取菜单类型。
            String type = stringValue(menu.get("type"));
            // 菜单树隐藏按钮时使用枚举判断节点类型。
            if (!includeButtons && PlatformMenuType.BUTTON.matches(type)) {
                // 跳过按钮节点。
                continue;
            }
            // 复制菜单节点，避免直接修改仓储返回对象。
            Map<String, Object> copy = new LinkedHashMap<>(menu);
            // 初始化子节点集合。
            copy.put("children", new ArrayList<Map<String, Object>>());
            // 将菜单节点放入有序索引。
            byId.put(stringValue(copy.get("id")), copy);
        }

        // 创建根节点列表。
        List<Map<String, Object>> roots = new ArrayList<>();
        // 遍历已索引菜单节点。
        for (Map<String, Object> menu : byId.values()) {
            // 读取父节点标识。
            String parentId = stringValue(menu.get("parentId"));
            // 父节点存在时挂载到父节点 children。
            if (StringUtils.isNotEmpty(parentId) && byId.containsKey(parentId)) {
                // 将当前节点加入父节点 children。
                childList(byId.get(parentId)).add(menu);
            } else {
                // 父节点不存在时作为根节点。
                roots.add(menu);
            }
        }
        // 按排序号递归排序菜单树。
        sortTree(roots);
        // 返回菜单树根节点。
        return roots;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param menu 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childList(Map<String, Object> menu) {
        // 从菜单节点中读取 children 字段并转换为列表。
        return (List<Map<String, Object>>) menu.get("children");
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param menus 业务处理所需参数。
     */
    private void sortTree(List<Map<String, Object>> menus) {
        // 按排序号升序排序当前层级节点。
        menus.sort(Comparator.comparingInt(item -> intValue(item.get("sortNo"))));
        // 遍历当前层级节点。
        for (Map<String, Object> menu : menus) {
            // 递归排序子节点。
            sortTree(childList(menu));
        }
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调平台管理相关仓储和领域规则。
     *
     * @param tree 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> toTreeSelect(List<Map<String, Object>> tree) {
        // 创建选择器节点结果列表。
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历菜单树节点。
        for (Map<String, Object> item : tree) {
            // 创建选择器节点。
            Map<String, Object> node = new LinkedHashMap<>();
            // 写入节点标识。
            node.put("id", item.get("id"));
            // 写入节点展示名称。
            node.put("label", item.get("name"));
            // 写入节点类型。
            node.put("type", item.get("type"));
            // 递归写入子节点。
            node.put("children", toTreeSelect(childList(item)));
            // 加入选择器节点结果列表。
            result.add(node);
        }
        // 返回选择器树。
        return result;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param rawPassword 业务处理所需参数。
     * @param passwordHash 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private boolean matches(String rawPassword, String passwordHash) {
        // BCrypt 摘要交给 Spring PasswordEncoder 校验。
        if (passwordHash.startsWith("$2")) {
            // 返回 BCrypt 密码校验结果。
            return passwordEncoder.matches(rawPassword, passwordHash);
        }
        // 兼容历史明文密码数据。
        return rawPassword.equals(passwordHash);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private PlatformUserRequest prepareUserRequest(PlatformUserRequest request) {
        PlatformUserRequest safeRequest = request == null ? new PlatformUserRequest() : request;
        String password = stringValue(safeRequest.getPassword());
        if (StringUtils.isNotBlank(password)) {
            safeRequest.setPasswordHash(passwordEncoder.encode(password));
            safeRequest.setPasswordAlgo("BCrypt");
            safeRequest.setPasswordUpdateTime(new Date());
        }
        return safeRequest;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param user 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> publicUser(Map<String, Object> user) {
        // 复制用户字段集合。
        Map<String, Object> result = new LinkedHashMap<>(user);
        // 移除密码摘要字段。
        result.remove("passwordHash");
        // 返回安全用户字段集合。
        return result;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param preference 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> legacyAppearanceResult(Map<String, Object> preference) {
        // 读取旧版主题值。
        String legacyTheme = stringValue(preference.get("preferenceValue"));
        // 将旧版主题映射为新版外观偏好结构。
        return appearanceResult(
                DEFAULT_COLOR_MODE.getCode(),
                !LEGACY_PROFESSIONAL_THEME.equals(legacyTheme),
                DEFAULT_ACCENT_COLOR,
                LEGACY_PROFESSIONAL_THEME.equals(legacyTheme)
                        ? AppearanceVisualStyle.FLAT.getCode()
                        : DEFAULT_VISUAL_STYLE.getCode()
                );
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param colorMode 业务处理所需参数。
     * @param fluidEnabled 业务处理所需参数。
     * @param accentColor 业务处理所需参数。
     * @param visualStyle 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> appearanceResult(
            String colorMode,
            boolean fluidEnabled,
            String accentColor,
            String visualStyle
    ) {
        // 创建外观偏好返回结构。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入颜色模式。
        result.put("colorMode", colorMode);
        // 写入流体布局开关。
        result.put("fluidEnabled", fluidEnabled);
        // 写入规范化后的强调色。
        result.put("accentColor", normalizeAccentColor(accentColor));
        // 写入规范化后的视觉风格。
        result.put("visualStyle", normalizeVisualStyle(visualStyle));
        // 返回外观偏好结构。
        return result;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param visualStyle 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String normalizeVisualStyle(String visualStyle) {
        // 使用视觉风格枚举校验并规范化视觉风格编码。
        return AppearanceVisualStyle.fromCode(visualStyle)
                .orElse(DEFAULT_VISUAL_STYLE)
                .getCode();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param accentColor 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private String normalizeAccentColor(String accentColor) {
        // 强调色存在时进行格式校验。
        if (accentColor != null) {
            // 去除强调色两侧空白。
            String trimmed = accentColor.trim();
            // 强调色符合十六进制色值格式时返回小写形式。
            if (ACCENT_COLOR_PATTERN.matcher(trimmed).matches()) {
                // 返回小写强调色。
                return trimmed.toLowerCase();
            }
        }
        // 强调色无效时返回默认强调色。
        return DEFAULT_ACCENT_COLOR;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    private Map<String, Object> fallbackUser() {
        // 创建默认管理员字段集合。
        Map<String, Object> user = new HashMap<>();
        // 写入默认管理员标识。
        user.put("id", DEFAULT_ADMIN_USER_ID);
        // 写入默认管理员用户名。
        user.put("username", "admin");
        // 写入默认管理员昵称。
        user.put("nickname", "超级管理员");
        // 写入默认租户标识。
        user.put("tenantId", "1");
        // 写入默认部门标识。
        user.put("deptId", "1");
        // 写入默认用户状态。
        user.put("status", UserStatus.ACTIVE.name());
        // 写入兜底记录创建时间。
        user.put("createTime", Instant.now().toString());
        // 返回默认管理员字段集合。
        return user;
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    private String resolvedUserId(String userId) {
        // 将用户标识转换为去空白字符串。
        String resolved = stringValue(userId);
        // 用户标识为空时使用默认管理员标识。
        return StringUtils.isEmpty(resolved) ? DEFAULT_ADMIN_USER_ID : resolved;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param users 业务处理所需参数。
     * @param currentUserId 业务记录标识。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> filterUsersByDataScope(
            List<Map<String, Object>> users,
            String currentUserId
    ) {
        // 解析当前用户标识。
        String userId = resolvedUserId(currentUserId);
        // 查询当前用户数据权限范围。
        String scope = resolveDataScope(userId);
        // 全部数据权限直接返回用户列表。
        if (DataScope.ALL.matches(scope)) {
            // 返回未过滤的用户列表。
            return users;
        }
        // 本人数据权限仅保留当前用户。
        if (DataScope.SELF.matches(scope)) {
            // 过滤出当前用户记录。
            return users.stream()
                    .filter(user -> userId.equals(stringValue(user.get("id"))))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 查询当前用户资料。
        Map<String, Object> currentUser = platformUserRepository.findUserById(userId)
                .map(DomainRecord::toMap)
                .orElse(Collections.emptyMap());
        // 读取当前用户部门标识。
        String currentDeptId = stringValue(currentUser.get("deptId"));
        // 创建允许访问的部门标识集合。
        Set<String> allowedDeptIds = new HashSet<>();
        // 自定义数据权限读取角色绑定部门。
        if (DataScope.CUSTOM.matches(scope)) {
            // 加入自定义授权部门标识。
            allowedDeptIds.addAll(platformPermissionRepository.listCustomDeptIdsByUserId(userId));
        } else if (DataScope.DEPT.matches(scope) || DataScope.DEPT_AND_CHILD.matches(scope)) {
            // 部门数据权限先加入当前部门。
            if (StringUtils.isNotEmpty(currentDeptId)) {
                // 加入当前部门标识。
                allowedDeptIds.add(currentDeptId);
            }
            // 部门及以下数据权限继续收集子部门。
            if (DataScope.DEPT_AND_CHILD.matches(scope)) {
                // 递归收集子部门标识。
                collectChildDeptIds(currentDeptId, allowedDeptIds);
            }
        }
        // 按允许部门集合过滤用户列表。
        return users.stream()
                .filter(user -> allowedDeptIds.contains(stringValue(user.get("deptId"))))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param rootDeptId 业务记录标识。
     * @param deptIds 业务处理所需参数。
     */
    private void collectChildDeptIds(String rootDeptId, Set<String> deptIds) {
        // 根部门为空时无需继续收集。
        if (StringUtils.isEmpty(rootDeptId)) {
            // 直接结束子部门收集。
            return;
        }
        // 查询全部部门记录。
        List<Map<String, Object>> departments = maps(platformOrganizationRepository.listDepartments());
        // 声明本轮是否有新增部门。
        boolean changed;
        // 循环扩展部门集合直到不再新增。
        do {
            // 默认本轮没有新增部门。
            changed = false;
            // 遍历全部部门记录。
            for (Map<String, Object> department : departments) {
                // 读取部门标识。
                String id = stringValue(department.get("id"));
                // 读取父部门标识。
                String parentId = stringValue(department.get("parentId"));
                // 父部门已允许且当前部门首次加入时标记发生变化。
                if (deptIds.contains(parentId) && deptIds.add(id)) {
                    // 标记本轮新增了部门。
                    changed = true;
                }
            }
            // 存在新增部门时继续下一轮扩展。
        } while (changed);
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调平台管理相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
     */
    private String resolveDataScope(String userId) {
        // 默认管理员拥有全部数据权限。
        if (DEFAULT_ADMIN_USER_ID.equals(userId)) {
            // 返回全部数据权限编码。
            return DataScope.ALL.getCode();
        }
        // 查询用户关联角色的数据权限编码列表。
        List<String> scopes = platformPermissionRepository.listDataScopesByUserId(userId);
        // 使用数据权限枚举解析最高权限范围。
        return DataScope.resolve(scopes).getCode();
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调平台管理相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        // 空值转换为空字符串，非空值转换为字符串并去除两侧空白。
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private int intValue(Object value) {
        // 数字类型直接读取整数值。
        if (value instanceof Number) {
            // 返回数字对象的整数值。
            return ((Number) value).intValue();
        }
        // 尝试按字符串解析整数。
        try {
            // 返回字符串解析后的整数。
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException ignored) {
            // 解析失败时返回默认排序值。
            return 0;
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @param fieldName 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private boolean booleanValue(Object value, String fieldName) {
        // 布尔类型直接返回。
        if (value instanceof Boolean) {
            // 返回原始布尔值。
            return (Boolean) value;
        }
        // 将原始值转换为字符串。
        String text = stringValue(value);
        // true 或 false 字符串按忽略大小写方式解析。
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            // 返回字符串解析后的布尔值。
            return Boolean.parseBoolean(text);
        }
        // 无法解析为布尔值时抛出参数异常。
        throw new IllegalArgumentException(fieldName + " 必须是布尔值");
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调平台管理相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 逐条导出领域记录的字段副本。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调平台管理相关仓储和领域规则。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Optional<Map<String, Object>> optionalMap(Optional<DomainRecord> record) {
        // 在记录存在时导出字段副本。
        return record.map(DomainRecord::toMap);
    }

    /**
     * 定义平台管理固定取值，集中约束业务状态和类型。
     */
    private enum AppearanceColorMode {

        LIGHT("light"),

        DARK("dark"),

        SYSTEM("system");

        private final String code;

        AppearanceColorMode(String code) {
            // 保存颜色模式编码。
            this.code = code;
        }

        /**
         * 处理当前业务用例，保持调用方不感知内部实现细节。
         *
         * @param code 业务处理所需参数。
         * @return 处理后的业务结果。
         */
        private static Optional<AppearanceColorMode> fromCode(String code) {
            // 遍历全部颜色模式枚举。
            for (AppearanceColorMode colorMode : values()) {
                // 命中颜色模式编码时返回枚举值。
                if (colorMode.code.equals(code)) {
                    // 返回匹配到的颜色模式枚举。
                    return Optional.of(colorMode);
                }
            }
            // 未命中颜色模式编码时返回空值。
            return Optional.empty();
        }

        /**
         * 查询业务数据详情，供上层用例继续编排或返回给调用方。
         *
         * @return 处理后的业务结果。
         */
        private String getCode() {
            // 返回当前颜色模式编码。
            return code;
        }
    }

    /**
     * 定义平台管理固定取值，集中约束业务状态和类型。
     */
    private enum AppearanceVisualStyle {

        FLAT("flat"),

        GLASS("glass");

        private final String code;

        AppearanceVisualStyle(String code) {
            // 保存视觉风格编码。
            this.code = code;
        }

        /**
         * 处理当前业务用例，保持调用方不感知内部实现细节。
         *
         * @param code 业务处理所需参数。
         * @return 处理后的业务结果。
         */
        private static Optional<AppearanceVisualStyle> fromCode(String code) {
            // 遍历全部视觉风格枚举。
            for (AppearanceVisualStyle visualStyle : values()) {
                // 命中视觉风格编码时返回枚举值。
                if (visualStyle.code.equals(code)) {
                    // 返回匹配到的视觉风格枚举。
                    return Optional.of(visualStyle);
                }
            }
            // 未命中视觉风格编码时返回空值。
            return Optional.empty();
        }

        /**
         * 查询业务数据详情，供上层用例继续编排或返回给调用方。
         *
         * @return 处理后的业务结果。
         */
        private String getCode() {
            // 返回当前视觉风格编码。
            return code;
        }
    }
}
