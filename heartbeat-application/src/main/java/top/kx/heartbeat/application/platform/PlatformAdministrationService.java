// 注释：声明当前文件所属的包路径。
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
 * 平台后台管理应用服务。
 *
 * <p>负责用户、菜单、角色、租户、系统配置等后台资源的应用层编排。</p>
 */

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class PlatformAdministrationService {

    /**
     * 默认超级管理员用户标识。
     */
    // 注释：声明当前成员或方法。
    private static final String DEFAULT_ADMIN_USER_ID = "1";

    /**
     * 旧版外观主题偏好键。
     */
    // 注释：声明当前成员或方法。
    private static final String LEGACY_APPEARANCE_THEME_KEY = "appearance.theme";

    /**
     * 外观颜色模式偏好键。
     */
    // 注释：声明当前成员或方法。
    private static final String APPEARANCE_COLOR_MODE_KEY = "appearance.colorMode";

    /**
     * 外观流体布局偏好键。
     */
    // 注释：声明当前成员或方法。
    private static final String APPEARANCE_FLUID_ENABLED_KEY = "appearance.fluidEnabled";

    /**
     * 外观强调色偏好键。
     */
    // 注释：声明当前成员或方法。
    private static final String APPEARANCE_ACCENT_COLOR_KEY = "appearance.accentColor";

    /**
     * 外观视觉风格偏好键。
     */
    // 注释：声明当前成员或方法。
    private static final String APPEARANCE_VISUAL_STYLE_KEY = "appearance.visualStyle";

    /**
     * 默认颜色模式。
     */
    // 注释：声明当前成员或方法。
    private static final AppearanceColorMode DEFAULT_COLOR_MODE = AppearanceColorMode.DARK;

    /**
     * 默认是否启用流体布局。
     */
    // 注释：声明当前成员或方法。
    private static final boolean DEFAULT_FLUID_ENABLED = true;

    /**
     * 默认强调色。
     */
    // 注释：声明当前成员或方法。
    private static final String DEFAULT_ACCENT_COLOR = "#1677ff";

    /**
     * 默认视觉风格。
     */
    // 注释：声明当前成员或方法。
    private static final AppearanceVisualStyle DEFAULT_VISUAL_STYLE = AppearanceVisualStyle.GLASS;

    /**
     * 旧版专业主题标识。
     */
    // 注释：声明当前成员或方法。
    private static final String LEGACY_PROFESSIONAL_THEME = "professional";

    /**
     * 强调色格式表达式。
     */
    // 注释：声明当前成员或方法。
    private static final Pattern ACCENT_COLOR_PATTERN =
            // 注释：执行当前代码行。
            Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformUserRepository platformUserRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformPermissionRepository platformPermissionRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformMenuRepository platformMenuRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformOrganizationRepository platformOrganizationRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformRoleRepository platformRoleRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformConfigRepository platformConfigRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformSocialRepository platformSocialRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformAuditQueryRepository platformAuditQueryRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformNoticeRepository platformNoticeRepository;

    /**
     * 平台登录日志仓储。
     */
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformLoginLogRepository platformLoginLogRepository;

    /**
     * 认证会话应用服务。
     */
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthenticationSessionService authenticationSessionService;

    /**
     * 当前用户提供器。
     */
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private CurrentUserProvider currentUserProvider;

    /**
     * 密码编码器。
     */
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PasswordEncoder passwordEncoder;

    /**
     * 查询当前登录用户资料。
     *
     * @return 当前登录用户资料。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse currentUser() {
        // 使用当前登录用户标识查询用户资料。
        // 注释：返回当前处理结果。
        return currentUser(currentUserProvider.currentUserId());
        // 注释：结束当前代码块。
    }

    /**
     * 查询指定用户资料。
     *
     * @param userId 用户标识。
     * @return 指定用户资料。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse currentUser(String userId) {
        // 解析用户标识，空值时回落到默认管理员。
        // 注释：设置或计算当前变量值。
        String resolvedUserId = resolvedUserId(userId);
        // 查询用户行数据，未查到时使用内置管理员兜底。
        // 注释：设置或计算当前变量值。
        Map<String, Object> user = platformUserRepository.findUserById(resolvedUserId)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElseGet(this::fallbackUser);
        // 移除敏感字段后构建接口返回结构。
        // 注释：设置或计算当前变量值。
        Map<String, Object> result = publicUser(user);
        // 附加当前用户角色编码列表。
        // 注释：执行当前代码行。
        result.put("roles", platformPermissionRepository.listRoleCodesByUserId(resolvedUserId));
        // 附加当前用户权限编码列表。
        // 注释：执行当前代码行。
        result.put("permissions", platformPermissionRepository.listPermissionsByUserId(String.valueOf(user.get("id"))));
        // 附加当前用户数据权限范围。
        // 注释：执行当前代码行。
        result.put("dataScope", resolveDataScope(resolvedUserId));
        // 返回当前用户资料。
        // 注释：返回当前处理结果。
        return RecordResponse.from(result);
        // 注释：结束当前代码块。
    }

    /**
     * 查询当前登录用户外观偏好。
     *
     * @return 当前登录用户外观偏好。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse appearancePreference() {
        // 使用当前登录用户标识查询外观偏好。
        // 注释：返回当前处理结果。
        return appearancePreference(currentUserProvider.currentUserId());
        // 注释：结束当前代码块。
    }

    /**
     * 查询指定用户外观偏好。
     *
     * @param userId 用户标识。
     * @return 指定用户外观偏好。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse appearancePreference(String userId) {
        // 解析用户标识，空值时回落到默认管理员。
        // 注释：设置或计算当前变量值。
        String resolvedUserId = resolvedUserId(userId);
        // 查询颜色模式偏好。
        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> colorPreference =
                // 注释：执行当前代码行。
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_COLOR_MODE_KEY));
        // 查询流体布局偏好。
        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> fluidPreference =
                // 注释：执行当前代码行。
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_FLUID_ENABLED_KEY));
        // 查询强调色偏好。
        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> accentPreference =
                // 注释：执行当前代码行。
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_ACCENT_COLOR_KEY));
        // 查询视觉风格偏好。
        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> visualPreference =
                // 注释：执行当前代码行。
                optionalMap(platformUserRepository.findUserPreference(resolvedUserId, APPEARANCE_VISUAL_STYLE_KEY));

        // 新版偏好任意一项存在时按新版结构返回。
        // 注释：判断当前业务条件。
        if (colorPreference.isPresent() || fluidPreference.isPresent() || accentPreference.isPresent()
                // 注释：执行当前代码行。
                || visualPreference.isPresent()) {
            // 解析颜色模式，缺省时使用默认颜色模式。
            // 注释：设置或计算当前变量值。
            String colorMode = colorPreference
                    // 注释：继续当前链式调用。
                    .map(preference -> stringValue(preference.get("preferenceValue")))
                    // 注释：继续当前链式调用。
                    .orElse(DEFAULT_COLOR_MODE.getCode());
            // 解析流体布局开关，缺省时使用默认流体布局开关。
            // 注释：设置或计算当前变量值。
            boolean fluidEnabled = fluidPreference
                    // 注释：继续当前链式调用。
                    .map(preference -> Boolean.parseBoolean(stringValue(preference.get("preferenceValue"))))
                    // 注释：继续当前链式调用。
                    .orElse(DEFAULT_FLUID_ENABLED);
            // 解析强调色，缺省时使用默认强调色。
            // 注释：设置或计算当前变量值。
            String accentColor = accentPreference
                    // 注释：继续当前链式调用。
                    .map(preference -> normalizeAccentColor(stringValue(preference.get("preferenceValue"))))
                    // 注释：继续当前链式调用。
                    .orElse(DEFAULT_ACCENT_COLOR);
            // 解析视觉风格，缺省时使用默认视觉风格。
            // 注释：设置或计算当前变量值。
            String visualStyle = visualPreference
                    // 注释：继续当前链式调用。
                    .map(preference -> normalizeVisualStyle(stringValue(preference.get("preferenceValue"))))
                    // 注释：继续当前链式调用。
                    .orElse(DEFAULT_VISUAL_STYLE.getCode());
            // 返回新版外观偏好结构。
            // 注释：返回当前处理结果。
            return RecordResponse.from(appearanceResult(colorMode, fluidEnabled, accentColor, visualStyle));
            // 注释：结束当前代码块。
        }

        // 新版偏好不存在时兼容读取旧版主题偏好。
        // 注释：返回当前处理结果。
        return platformUserRepository.findUserPreference(resolvedUserId, LEGACY_APPEARANCE_THEME_KEY)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .map(this::legacyAppearanceResult)
                // 注释：继续当前链式调用。
                .map(RecordResponse::from)
                // 注释：继续当前链式调用。
                .orElseGet(() -> RecordResponse.from(appearanceResult(
                        // 注释：执行当前代码行。
                        DEFAULT_COLOR_MODE.getCode(),
                        // 注释：执行当前代码行。
                        DEFAULT_FLUID_ENABLED,
                        // 注释：执行当前代码行。
                        DEFAULT_ACCENT_COLOR,
                        // 注释：执行当前代码行。
                        DEFAULT_VISUAL_STYLE.getCode()
                        // 注释：结束当前多行调用。
                )));
        // 注释：结束当前代码块。
    }

    /**
     * 更新当前登录用户外观偏好。
     *
     * @param command 外观偏好更新命令。
     * @return 更新后的外观偏好。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateAppearancePreference(PlatformAppearancePreferenceRequest request) {
        // 使用当前登录用户标识更新外观偏好。
        // 注释：返回当前处理结果。
        return updateAppearancePreference(currentUserProvider.currentUserId(), request);
        // 注释：结束当前代码块。
    }

    /**
     * 更新指定用户外观偏好。
     *
     * @param userId 用户标识。
     * @param command 外观偏好更新命令。
     * @return 更新后的外观偏好。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateAppearancePreference(String userId, PlatformAppearancePreferenceRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformAppearancePreferenceRequest safeRequest =
                // 注释：设置或计算当前变量值。
                request == null ? new PlatformAppearancePreferenceRequest() : request;
        // 查询当前外观偏好作为局部更新的默认值来源。
        // 注释：设置或计算当前变量值。
        Map<String, Object> current = appearancePreference(userId).toMap();
        // 解析颜色模式入参，未传时沿用当前值。
        // 注释：设置或计算当前变量值。
        String colorMode = safeRequest.getColorMode() != null
                // 注释：执行当前代码行。
                ? stringValue(safeRequest.getColorMode())
                // 注释：执行当前代码行。
                : stringValue(current.get("colorMode"));
        // 解析流体布局入参，未传时沿用当前值。
        // 注释：设置或计算当前变量值。
        boolean fluidEnabled = safeRequest.getFluidEnabled() != null
                // 注释：执行当前代码行。
                ? safeRequest.getFluidEnabled()
                // 注释：执行当前代码行。
                : Boolean.TRUE.equals(current.get("fluidEnabled"));
        // 解析强调色入参，未传时沿用当前值。
        // 注释：设置或计算当前变量值。
        String accentColor = safeRequest.getAccentColor() != null
                // 注释：执行当前代码行。
                ? normalizeAccentColor(stringValue(safeRequest.getAccentColor()))
                // 注释：执行当前代码行。
                : stringValue(current.get("accentColor"));
        // 解析视觉风格入参，未传时沿用当前值。
        // 注释：设置或计算当前变量值。
        String visualStyle = safeRequest.getVisualStyle() != null
                // 注释：执行当前代码行。
                ? normalizeVisualStyle(stringValue(safeRequest.getVisualStyle()))
                // 注释：执行当前代码行。
                : stringValue(current.get("visualStyle"));

        // 固化原始颜色模式入参，供异常消息安全引用。
        // 注释：设置或计算当前变量值。
        String requestedColorMode = colorMode;
        // 使用颜色模式枚举校验并规范化颜色模式编码。
        // 注释：设置或计算当前变量值。
        colorMode = AppearanceColorMode.fromCode(requestedColorMode)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("不支持的颜色模式: " + requestedColorMode))
                // 注释：继续当前链式调用。
                .getCode();

        // 解析用户标识，空值时回落到默认管理员。
        // 注释：设置或计算当前变量值。
        String resolvedUserId = resolvedUserId(userId);
        // 保存颜色模式偏好。
        // 注释：执行当前代码行。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_COLOR_MODE_KEY, colorMode);
        // 保存流体布局偏好。
        // 注释：执行当前代码行。
        platformUserRepository.saveUserPreference(
                // 注释：执行当前代码行。
                resolvedUserId,
                // 注释：执行当前代码行。
                APPEARANCE_FLUID_ENABLED_KEY,
                // 注释：执行当前代码行。
                String.valueOf(fluidEnabled)
                // 注释：结束当前表达式。
        );
        // 保存强调色偏好。
        // 注释：执行当前代码行。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_ACCENT_COLOR_KEY, accentColor);
        // 保存视觉风格偏好。
        // 注释：执行当前代码行。
        platformUserRepository.saveUserPreference(resolvedUserId, APPEARANCE_VISUAL_STYLE_KEY, visualStyle);
        // 返回更新后的外观偏好。
        // 注释：返回当前处理结果。
        return RecordResponse.from(appearanceResult(colorMode, fluidEnabled, accentColor, visualStyle));
        // 注释：结束当前代码块。
    }

    /**
     * 使用账号密码登录后台平台。
     *
     * @param command 登录命令。
     * @return 登录结果。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public LoginResponse login(PlatformLoginRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformLoginRequest safeRequest = request == null ? new PlatformLoginRequest() : request;
        // 读取登录用户名。
        // 注释：设置或计算当前变量值。
        String username = stringValue(safeRequest.getUsername());
        // 读取登录密码。
        // 注释：设置或计算当前变量值。
        String password = stringValue(safeRequest.getPassword());
        // 按用户名查询用户记录。
        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> userOptional = optionalMap(platformUserRepository.findUserByUsername(username));
        // 用户不存在或密码不匹配时记录失败日志并中断登录。
        // 注释：判断当前业务条件。
        if (!userOptional.isPresent() || !matches(password, stringValue(userOptional.get().get("passwordHash")))) {
            // 写入失败登录日志。
            // 注释：执行当前代码行。
            platformLoginLogRepository.recordLogin(username, LoginResultStatus.FAIL.getCode(), "用户名或密码错误");
            // 抛出用户名或密码错误异常。
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("用户名或密码错误");
            // 注释：结束当前代码块。
        }

        // 写入成功登录日志。
        // 注释：执行当前代码行。
        platformLoginLogRepository.recordLogin(username, LoginResultStatus.SUCCESS.getCode(), "账号密码登录成功");
        // 过滤用户敏感字段。
        // 注释：设置或计算当前变量值。
        Map<String, Object> user = publicUser(userOptional.get());
        // 创建认证会话并复制为可扩展返回结构。
        // 注释：设置或计算当前变量值。
        AuthTokenResponse tokens = authenticationSessionService.createSession(
                // 注释：执行当前代码行。
                String.valueOf(user.get("id")),
                // 注释：执行当前代码行。
                String.valueOf(user.get("username")),
                // 注释：执行当前代码行。
                String.valueOf(user.get("tenantId")));
        // 返回登录结果。
        // 注释：返回当前处理结果。
        return LoginResponse.of(
                // 注释：执行当前代码行。
                tokens,
                // 注释：执行当前代码行。
                RecordResponse.from(user),
                // 注释：执行当前代码行。
                platformPermissionRepository.listPermissionsByUserId(String.valueOf(user.get("id")))
                // 注释：结束当前表达式。
        );
        // 注释：结束当前代码块。
    }

    /**
     * 查询完整菜单树。
     *
     * @return 完整菜单树。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listMenus() {
        // 查询菜单记录并构建包含按钮节点的树结构。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(buildTree(maps(platformMenuRepository.listMenus()), true));
        // 注释：结束当前代码块。
    }

    /**
     * 查询当前登录用户路由树。
     *
     * @return 当前登录用户路由树。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> routes() {
        // 使用当前登录用户标识查询路由树。
        // 注释：返回当前处理结果。
        return routes(currentUserProvider.currentUserId());
        // 注释：结束当前代码块。
    }

    /**
     * 查询指定用户路由树。
     *
     * @param userId 用户标识。
     * @return 指定用户路由树。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> routes(String userId) {
        // 查询授权菜单记录并构建不包含按钮节点的路由树。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(buildTree(maps(platformMenuRepository.listAuthorizedMenus(resolvedUserId(userId))), false));
        // 注释：结束当前代码块。
    }

    /**
     * 查询菜单树选择器数据。
     *
     * @return 菜单树选择器数据。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> menuTreeSelect() {
        // 构建完整菜单树。
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> tree = buildTree(maps(platformMenuRepository.listMenus()), true);
        // 将菜单树转换为选择器节点结构。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(toTreeSelect(tree));
        // 注释：结束当前代码块。
    }

    /**
     * 创建菜单。
     *
     * @param command 菜单创建命令。
     * @return 新建菜单记录。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createMenu(PlatformMenuRequest request) {
        // 委托仓储创建菜单并返回字段 Map。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformMenuRepository.createMenu(request));
        // 注释：结束当前代码块。
    }

    /**
     * 更新菜单。
     *
     * @param id 菜单标识。
     * @param command 菜单更新命令。
     * @return 更新后的菜单记录。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateMenu(String id, PlatformMenuRequest request) {
        // 委托仓储更新菜单并返回字段 Map。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformMenuRepository.updateMenu(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 删除菜单。
     *
     * @param id 菜单标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteMenu(String id) {
        // 委托仓储删除菜单。
        // 注释：执行当前代码行。
        platformMenuRepository.deleteMenu(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询角色菜单授权详情。
     *
     * @param roleId 角色标识。
     * @return 角色菜单授权详情。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse roleMenuDetail(String roleId) {
        // 角色不存在时抛出业务参数异常。
        // 注释：判断当前业务条件。
        if (!platformPermissionRepository.roleExists(roleId)) {
            // 抛出角色不存在异常。
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("角色不存在: " + roleId);
            // 注释：结束当前代码块。
        }
        // 创建角色菜单详情返回结构。
        // 注释：设置或计算当前变量值。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入角色标识。
        // 注释：执行当前代码行。
        result.put("roleId", roleId);
        // 写入角色已授权菜单标识列表。
        // 注释：执行当前代码行。
        result.put("menuIds", platformPermissionRepository.listRoleMenuIds(roleId));
        // 写入菜单树选择器数据。
        // 注释：执行当前代码行。
        result.put("menuTree", menuTreeSelect());
        // 返回角色菜单授权详情。
        // 注释：返回当前处理结果。
        return RecordResponse.from(result);
        // 注释：结束当前代码块。
    }

    /**
     * 分配角色菜单。
     *
     * @param roleId 角色标识。
     * @param menuIds 菜单标识列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void assignRoleMenus(String roleId, List<String> menuIds) {
        // 角色不存在时抛出业务参数异常。
        // 注释：判断当前业务条件。
        if (!platformPermissionRepository.roleExists(roleId)) {
            // 抛出角色不存在异常。
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("角色不存在: " + roleId);
            // 注释：结束当前代码块。
        }
        // 规范化菜单标识列表，去掉空值和重复值。
        // 注释：设置或计算当前变量值。
        List<String> normalized = menuIds == null
                // 注释：执行当前代码行。
                ? Collections.emptyList()
                // 注释：执行当前代码行。
                : menuIds.stream().map(this::stringValue).filter(id -> StringUtils.isNotEmpty(id)).distinct()
                // 注释：继续当前链式调用。
                .collect(java.util.stream.Collectors.toList());
        // 保存角色与菜单的授权关系。
        // 注释：执行当前代码行。
        platformPermissionRepository.saveRoleMenus(roleId, normalized);
        // 注释：结束当前代码块。
    }

    /**
     * 查询用户资源列表。
     *
     * @return 用户资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listUsers() {
        // 查询用户列表后按当前用户数据权限过滤并移除敏感字段。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(filterUsersByDataScope(maps(platformUserRepository.listUsers()), currentUserProvider.currentUserId()).stream()
                // 注释：继续当前链式调用。
                .map(this::publicUser)
                // 注释：继续当前链式调用。
                .collect(java.util.stream.Collectors.toList()));
        // 注释：结束当前代码块。
    }

    /**
     * 创建用户资源。
     *
     * @param command 用户创建命令。
     * @return 新建用户资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createUser(PlatformUserRequest request) {
        // 通过平台管理仓储创建用户记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformUserRepository.createUser(prepareUserRequest(request)));
        // 注释：结束当前代码块。
    }

    /**
     * 更新用户资源。
     *
     * @param id 用户标识。
     * @param command 用户更新命令。
     * @return 更新后的用户资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateUser(String id, PlatformUserRequest request) {
        // 通过平台管理仓储更新用户记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformUserRepository.updateUser(id, prepareUserRequest(request)));
        // 注释：结束当前代码块。
    }

    /**
     * 删除用户资源。
     *
     * @param id 用户标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteUser(String id) {
        // 通过平台管理仓储删除用户记录。
        // 注释：执行当前代码行。
        platformUserRepository.deleteUser(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询部门资源列表。
     *
     * @return 部门资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDepartments() {
        // 通过平台管理仓储查询部门列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listDepartments()));
        // 注释：结束当前代码块。
    }

    /**
     * 创建部门资源。
     *
     * @param command 部门创建命令。
     * @return 新建部门资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createDepartment(PlatformDepartmentRequest request) {
        // 通过平台管理仓储创建部门记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformOrganizationRepository.createDepartment(request));
        // 注释：结束当前代码块。
    }

    /**
     * 更新部门资源。
     *
     * @param id 部门标识。
     * @param command 部门更新命令。
     * @return 更新后的部门资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateDepartment(String id, PlatformDepartmentRequest request) {
        // 通过平台管理仓储更新部门记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformOrganizationRepository.updateDepartment(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 删除部门资源。
     *
     * @param id 部门标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteDepartment(String id) {
        // 通过平台管理仓储删除部门记录。
        // 注释：执行当前代码行。
        platformOrganizationRepository.deleteDepartment(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询角色资源列表。
     *
     * @return 角色资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listRoles() {
        // 通过平台管理仓储查询角色列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformRoleRepository.listRoles()));
        // 注释：结束当前代码块。
    }

    /**
     * 创建角色资源。
     *
     * @param command 角色创建命令。
     * @return 新建角色资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createRole(PlatformRoleRequest request) {
        // 通过平台管理仓储创建角色记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformRoleRepository.createRole(request));
        // 注释：结束当前代码块。
    }

    /**
     * 更新角色资源。
     *
     * @param id 角色标识。
     * @param command 角色更新命令。
     * @return 更新后的角色资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateRole(String id, PlatformRoleRequest request) {
        // 通过平台管理仓储更新角色记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformRoleRepository.updateRole(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 删除角色资源。
     *
     * @param id 角色标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteRole(String id) {
        // 通过平台管理仓储删除角色记录。
        // 注释：执行当前代码行。
        platformRoleRepository.deleteRole(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询参数配置资源列表。
     *
     * @return 参数配置资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listConfigurations() {
        // 通过平台管理仓储查询参数配置列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listConfigurations()));
        // 注释：结束当前代码块。
    }

    /**
     * 创建参数配置资源。
     *
     * @param command 参数配置创建命令。
     * @return 新建参数配置资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createConfiguration(PlatformConfigurationRequest request) {
        // 通过平台管理仓储创建参数配置记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformConfigRepository.createConfiguration(request));
        // 注释：结束当前代码块。
    }

    /**
     * 更新参数配置资源。
     *
     * @param id 参数配置标识。
     * @param command 参数配置更新命令。
     * @return 更新后的参数配置资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateConfiguration(String id, PlatformConfigurationRequest request) {
        // 通过平台管理仓储更新参数配置记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformConfigRepository.updateConfiguration(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 删除参数配置资源。
     *
     * @param id 参数配置标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteConfiguration(String id) {
        // 通过平台管理仓储删除参数配置记录。
        // 注释：执行当前代码行。
        platformConfigRepository.deleteConfiguration(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询社交登录渠道资源列表。
     *
     * @return 社交登录渠道资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listSocialProviders() {
        // 通过平台管理仓储查询社交登录渠道列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformSocialRepository.listSocialProviders()));
        // 注释：结束当前代码块。
    }

    /**
     * 创建社交登录渠道资源。
     *
     * @param command 社交登录渠道创建命令。
     * @return 新建社交登录渠道资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse createSocialProvider(PlatformSocialProviderRequest request) {
        // 通过平台管理仓储创建社交登录渠道记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformSocialRepository.createSocialProvider(request));
        // 注释：结束当前代码块。
    }

    /**
     * 更新社交登录渠道资源。
     *
     * @param id 社交登录渠道标识。
     * @param command 社交登录渠道更新命令。
     * @return 更新后的社交登录渠道资源。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse updateSocialProvider(String id, PlatformSocialProviderRequest request) {
        // 通过平台管理仓储更新社交登录渠道记录。
        // 注释：返回当前处理结果。
        return RecordResponse.from(platformSocialRepository.updateSocialProvider(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 删除社交登录渠道资源。
     *
     * @param id 社交登录渠道标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public void deleteSocialProvider(String id) {
        // 通过平台管理仓储删除社交登录渠道记录。
        // 注释：执行当前代码行。
        platformSocialRepository.deleteSocialProvider(id);
        // 注释：结束当前代码块。
    }

    /**
     * 查询登录日志资源列表。
     *
     * @return 登录日志资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listLoginLogs() {
        // 通过平台管理仓储查询登录日志列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listLoginLogs()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询租户资源列表。
     *
     * @return 租户资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listTenants() {
        // 通过平台管理仓储查询租户列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listTenants()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询岗位资源列表。
     *
     * @return 岗位资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listPosts() {
        // 通过平台管理仓储查询岗位列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformOrganizationRepository.listPosts()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询字典类型资源列表。
     *
     * @return 字典类型资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDictTypes() {
        // 通过平台管理仓储查询字典类型列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listDictTypes()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询字典数据资源列表。
     *
     * @return 字典数据资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listDictData() {
        // 通过平台管理仓储查询字典数据列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformConfigRepository.listDictData()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询通知公告资源列表。
     *
     * @return 通知公告资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listNotices() {
        // 通过平台管理仓储查询通知公告列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformNoticeRepository.listNotices()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询操作日志资源列表。
     *
     * @return 操作日志资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listOperationLogs() {
        // 通过平台管理仓储查询操作日志列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOperationLogs()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询在线会话资源列表。
     *
     * @return 在线会话资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listOnlineSessions() {
        // 通过平台管理仓储查询在线会话列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOnlineSessions()));
        // 注释：结束当前代码块。
    }

    /**
     * 查询 OAuth 客户端资源列表。
     *
     * @return OAuth 客户端资源列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listOauthClients() {
        // 通过平台管理仓储查询 OAuth 客户端列表。
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(platformAuditQueryRepository.listOauthClients()));
        // 注释：结束当前代码块。
    }

    /**
     * 构建菜单树。
     *
     * @param menus 菜单平铺列表。
     * @param includeButtons 是否包含按钮节点。
     * @return 菜单树。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> menus, boolean includeButtons) {
        // 创建菜单标识到菜单节点的有序索引。
        // 注释：设置或计算当前变量值。
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        // 遍历菜单平铺列表。
        // 注释：遍历当前数据集合。
        for (Map<String, Object> menu : menus) {
            // 读取菜单类型。
            // 注释：设置或计算当前变量值。
            String type = stringValue(menu.get("type"));
            // 菜单树隐藏按钮时使用枚举判断节点类型。
            // 注释：判断当前业务条件。
            if (!includeButtons && PlatformMenuType.BUTTON.matches(type)) {
                // 跳过按钮节点。
                // 注释：执行当前代码行。
                continue;
                // 注释：结束当前代码块。
            }
            // 复制菜单节点，避免直接修改仓储返回对象。
            // 注释：设置或计算当前变量值。
            Map<String, Object> copy = new LinkedHashMap<>(menu);
            // 初始化子节点集合。
            // 注释：执行当前代码行。
            copy.put("children", new ArrayList<Map<String, Object>>());
            // 将菜单节点放入有序索引。
            // 注释：执行当前代码行。
            byId.put(stringValue(copy.get("id")), copy);
            // 注释：结束当前代码块。
        }

        // 创建根节点列表。
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> roots = new ArrayList<>();
        // 遍历已索引菜单节点。
        // 注释：遍历当前数据集合。
        for (Map<String, Object> menu : byId.values()) {
            // 读取父节点标识。
            // 注释：设置或计算当前变量值。
            String parentId = stringValue(menu.get("parentId"));
            // 父节点存在时挂载到父节点 children。
            // 注释：判断当前业务条件。
            if (StringUtils.isNotEmpty(parentId) && byId.containsKey(parentId)) {
                // 将当前节点加入父节点 children。
                // 注释：执行当前代码行。
                childList(byId.get(parentId)).add(menu);
                // 注释：处理条件不满足时的分支。
            } else {
                // 父节点不存在时作为根节点。
                // 注释：执行当前代码行。
                roots.add(menu);
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 按排序号递归排序菜单树。
        // 注释：执行当前代码行。
        sortTree(roots);
        // 返回菜单树根节点。
        // 注释：返回当前处理结果。
        return roots;
        // 注释：结束当前代码块。
    }

    /**
     * 读取菜单子节点集合。
     *
     * @param menu 菜单节点。
     * @return 菜单子节点集合。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childList(Map<String, Object> menu) {
        // 从菜单节点中读取 children 字段并转换为列表。
        // 注释：返回当前处理结果。
        return (List<Map<String, Object>>) menu.get("children");
        // 注释：结束当前代码块。
    }

    /**
     * 递归排序菜单树。
     *
     * @param menus 菜单节点列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void sortTree(List<Map<String, Object>> menus) {
        // 按排序号升序排序当前层级节点。
        // 注释：执行当前代码行。
        menus.sort(Comparator.comparingInt(item -> intValue(item.get("sortNo"))));
        // 遍历当前层级节点。
        // 注释：遍历当前数据集合。
        for (Map<String, Object> menu : menus) {
            // 递归排序子节点。
            // 注释：执行当前代码行。
            sortTree(childList(menu));
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 将菜单树转换为选择器树。
     *
     * @param tree 菜单树。
     * @return 选择器树。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> toTreeSelect(List<Map<String, Object>> tree) {
        // 创建选择器节点结果列表。
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历菜单树节点。
        // 注释：遍历当前数据集合。
        for (Map<String, Object> item : tree) {
            // 创建选择器节点。
            // 注释：设置或计算当前变量值。
            Map<String, Object> node = new LinkedHashMap<>();
            // 写入节点标识。
            // 注释：执行当前代码行。
            node.put("id", item.get("id"));
            // 写入节点展示名称。
            // 注释：执行当前代码行。
            node.put("label", item.get("name"));
            // 写入节点类型。
            // 注释：执行当前代码行。
            node.put("type", item.get("type"));
            // 递归写入子节点。
            // 注释：执行当前代码行。
            node.put("children", toTreeSelect(childList(item)));
            // 加入选择器节点结果列表。
            // 注释：执行当前代码行。
            result.add(node);
            // 注释：结束当前代码块。
        }
        // 返回选择器树。
        // 注释：返回当前处理结果。
        return result;
        // 注释：结束当前代码块。
    }

    /**
     * 校验原始密码是否匹配密码摘要。
     *
     * @param rawPassword 原始密码。
     * @param passwordHash 密码摘要。
     * @return 是否匹配。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private boolean matches(String rawPassword, String passwordHash) {
        // BCrypt 摘要交给 Spring PasswordEncoder 校验。
        // 注释：判断当前业务条件。
        if (passwordHash.startsWith("$2")) {
            // 返回 BCrypt 密码校验结果。
            // 注释：返回当前处理结果。
            return passwordEncoder.matches(rawPassword, passwordHash);
            // 注释：结束当前代码块。
        }
        // 兼容历史明文密码数据。
        // 注释：返回当前处理结果。
        return rawPassword.equals(passwordHash);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private PlatformUserRequest prepareUserRequest(PlatformUserRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformUserRequest safeRequest = request == null ? new PlatformUserRequest() : request;
        // 注释：设置或计算当前变量值。
        String password = stringValue(safeRequest.getPassword());
        // 注释：判断当前业务条件。
        if (StringUtils.isNotBlank(password)) {
            // 注释：执行当前代码行。
            safeRequest.setPasswordHash(passwordEncoder.encode(password));
            // 注释：执行当前代码行。
            safeRequest.setPasswordAlgo("BCrypt");
            // 注释：执行当前代码行。
            safeRequest.setPasswordUpdateTime(new Date());
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return safeRequest;
        // 注释：结束当前代码块。
    }

    /**
     * 过滤用户敏感字段。
     *
     * @param user 用户字段集合。
     * @return 不包含敏感字段的用户字段集合。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> publicUser(Map<String, Object> user) {
        // 复制用户字段集合。
        // 注释：设置或计算当前变量值。
        Map<String, Object> result = new LinkedHashMap<>(user);
        // 移除密码摘要字段。
        // 注释：执行当前代码行。
        result.remove("passwordHash");
        // 返回安全用户字段集合。
        // 注释：返回当前处理结果。
        return result;
        // 注释：结束当前代码块。
    }

    /**
     * 转换旧版外观主题偏好。
     *
     * @param preference 旧版外观主题偏好。
     * @return 新版外观偏好。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> legacyAppearanceResult(Map<String, Object> preference) {
        // 读取旧版主题值。
        // 注释：设置或计算当前变量值。
        String legacyTheme = stringValue(preference.get("preferenceValue"));
        // 将旧版主题映射为新版外观偏好结构。
        // 注释：返回当前处理结果。
        return appearanceResult(
                // 注释：执行当前代码行。
                DEFAULT_COLOR_MODE.getCode(),
                // 注释：执行当前代码行。
                !LEGACY_PROFESSIONAL_THEME.equals(legacyTheme),
                // 注释：执行当前代码行。
                DEFAULT_ACCENT_COLOR,
                // 注释：执行当前代码行。
                LEGACY_PROFESSIONAL_THEME.equals(legacyTheme)
                        // 注释：执行当前代码行。
                        ? AppearanceVisualStyle.FLAT.getCode()
                        // 注释：执行当前代码行。
                        : DEFAULT_VISUAL_STYLE.getCode()
                // 注释：结束当前表达式。
                );
        // 注释：结束当前代码块。
    }

    /**
     * 构建外观偏好返回结构。
     *
     * @param colorMode 颜色模式。
     * @param fluidEnabled 是否启用流体布局。
     * @param accentColor 强调色。
     * @param visualStyle 视觉风格。
     * @return 外观偏好返回结构。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> appearanceResult(
            // 注释：执行当前代码行。
            String colorMode,
            // 注释：执行当前代码行。
            boolean fluidEnabled,
            // 注释：执行当前代码行。
            String accentColor,
            // 注释：执行当前代码行。
            String visualStyle
            // 注释：结束当前多行调用。
    ) {
        // 创建外观偏好返回结构。
        // 注释：设置或计算当前变量值。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入颜色模式。
        // 注释：执行当前代码行。
        result.put("colorMode", colorMode);
        // 写入流体布局开关。
        // 注释：执行当前代码行。
        result.put("fluidEnabled", fluidEnabled);
        // 写入规范化后的强调色。
        // 注释：执行当前代码行。
        result.put("accentColor", normalizeAccentColor(accentColor));
        // 写入规范化后的视觉风格。
        // 注释：执行当前代码行。
        result.put("visualStyle", normalizeVisualStyle(visualStyle));
        // 返回外观偏好结构。
        // 注释：返回当前处理结果。
        return result;
        // 注释：结束当前代码块。
    }

    /**
     * 规范化视觉风格。
     *
     * @param visualStyle 视觉风格。
     * @return 规范化后的视觉风格。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String normalizeVisualStyle(String visualStyle) {
        // 使用视觉风格枚举校验并规范化视觉风格编码。
        // 注释：返回当前处理结果。
        return AppearanceVisualStyle.fromCode(visualStyle)
                // 注释：继续当前链式调用。
                .orElse(DEFAULT_VISUAL_STYLE)
                // 注释：继续当前链式调用。
                .getCode();
        // 注释：结束当前代码块。
    }

    /**
     * 规范化强调色。
     *
     * @param accentColor 强调色。
     * @return 规范化后的强调色。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String normalizeAccentColor(String accentColor) {
        // 强调色存在时进行格式校验。
        // 注释：判断当前业务条件。
        if (accentColor != null) {
            // 去除强调色两侧空白。
            // 注释：设置或计算当前变量值。
            String trimmed = accentColor.trim();
            // 强调色符合十六进制色值格式时返回小写形式。
            // 注释：判断当前业务条件。
            if (ACCENT_COLOR_PATTERN.matcher(trimmed).matches()) {
                // 返回小写强调色。
                // 注释：返回当前处理结果。
                return trimmed.toLowerCase();
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 强调色无效时返回默认强调色。
        // 注释：返回当前处理结果。
        return DEFAULT_ACCENT_COLOR;
        // 注释：结束当前代码块。
    }

    /**
     * 构建默认管理员兜底用户。
     *
     * @return 默认管理员兜底用户。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Map<String, Object> fallbackUser() {
        // 创建默认管理员字段集合。
        // 注释：设置或计算当前变量值。
        Map<String, Object> user = new HashMap<>();
        // 写入默认管理员标识。
        // 注释：执行当前代码行。
        user.put("id", DEFAULT_ADMIN_USER_ID);
        // 写入默认管理员用户名。
        // 注释：执行当前代码行。
        user.put("username", "admin");
        // 写入默认管理员昵称。
        // 注释：执行当前代码行。
        user.put("nickname", "超级管理员");
        // 写入默认租户标识。
        // 注释：执行当前代码行。
        user.put("tenantId", "1");
        // 写入默认部门标识。
        // 注释：执行当前代码行。
        user.put("deptId", "1");
        // 写入默认用户状态。
        // 注释：执行当前代码行。
        user.put("status", UserStatus.ACTIVE.name());
        // 写入兜底记录创建时间。
        // 注释：执行当前代码行。
        user.put("createTime", Instant.now().toString());
        // 返回默认管理员字段集合。
        // 注释：返回当前处理结果。
        return user;
        // 注释：结束当前代码块。
    }

    /**
     * 解析用户标识。
     *
     * @param userId 用户标识。
     * @return 解析后的用户标识。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String resolvedUserId(String userId) {
        // 将用户标识转换为去空白字符串。
        // 注释：设置或计算当前变量值。
        String resolved = stringValue(userId);
        // 用户标识为空时使用默认管理员标识。
        // 注释：返回当前处理结果。
        return StringUtils.isEmpty(resolved) ? DEFAULT_ADMIN_USER_ID : resolved;
        // 注释：结束当前代码块。
    }

    /**
     * 按当前用户数据权限过滤用户列表。
     *
     * @param users 用户列表。
     * @param currentUserId 当前用户标识。
     * @return 数据权限过滤后的用户列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> filterUsersByDataScope(
            // 注释：执行当前代码行。
            List<Map<String, Object>> users,
            // 注释：执行当前代码行。
            String currentUserId
            // 注释：结束当前多行调用。
    ) {
        // 解析当前用户标识。
        // 注释：设置或计算当前变量值。
        String userId = resolvedUserId(currentUserId);
        // 查询当前用户数据权限范围。
        // 注释：设置或计算当前变量值。
        String scope = resolveDataScope(userId);
        // 全部数据权限直接返回用户列表。
        // 注释：判断当前业务条件。
        if (DataScope.ALL.matches(scope)) {
            // 返回未过滤的用户列表。
            // 注释：返回当前处理结果。
            return users;
            // 注释：结束当前代码块。
        }
        // 本人数据权限仅保留当前用户。
        // 注释：判断当前业务条件。
        if (DataScope.SELF.matches(scope)) {
            // 过滤出当前用户记录。
            // 注释：返回当前处理结果。
            return users.stream()
                    // 注释：继续当前链式调用。
                    .filter(user -> userId.equals(stringValue(user.get("id"))))
                    // 注释：继续当前链式调用。
                    .collect(java.util.stream.Collectors.toList());
            // 注释：结束当前代码块。
        }

        // 查询当前用户资料。
        // 注释：设置或计算当前变量值。
        Map<String, Object> currentUser = platformUserRepository.findUserById(userId)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElse(Collections.emptyMap());
        // 读取当前用户部门标识。
        // 注释：设置或计算当前变量值。
        String currentDeptId = stringValue(currentUser.get("deptId"));
        // 创建允许访问的部门标识集合。
        // 注释：设置或计算当前变量值。
        Set<String> allowedDeptIds = new HashSet<>();
        // 自定义数据权限读取角色绑定部门。
        // 注释：判断当前业务条件。
        if (DataScope.CUSTOM.matches(scope)) {
            // 加入自定义授权部门标识。
            // 注释：执行当前代码行。
            allowedDeptIds.addAll(platformPermissionRepository.listCustomDeptIdsByUserId(userId));
            // 注释：处理条件不满足时的分支。
        } else if (DataScope.DEPT.matches(scope) || DataScope.DEPT_AND_CHILD.matches(scope)) {
            // 部门数据权限先加入当前部门。
            // 注释：判断当前业务条件。
            if (StringUtils.isNotEmpty(currentDeptId)) {
                // 加入当前部门标识。
                // 注释：执行当前代码行。
                allowedDeptIds.add(currentDeptId);
                // 注释：结束当前代码块。
            }
            // 部门及以下数据权限继续收集子部门。
            // 注释：判断当前业务条件。
            if (DataScope.DEPT_AND_CHILD.matches(scope)) {
                // 递归收集子部门标识。
                // 注释：执行当前代码行。
                collectChildDeptIds(currentDeptId, allowedDeptIds);
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 按允许部门集合过滤用户列表。
        // 注释：返回当前处理结果。
        return users.stream()
                // 注释：继续当前链式调用。
                .filter(user -> allowedDeptIds.contains(stringValue(user.get("deptId"))))
                // 注释：继续当前链式调用。
                .collect(java.util.stream.Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 收集指定部门下的所有子部门标识。
     *
     * @param rootDeptId 根部门标识。
     * @param deptIds 部门标识集合。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void collectChildDeptIds(String rootDeptId, Set<String> deptIds) {
        // 根部门为空时无需继续收集。
        // 注释：判断当前业务条件。
        if (StringUtils.isEmpty(rootDeptId)) {
            // 直接结束子部门收集。
            // 注释：返回当前处理结果。
            return;
            // 注释：结束当前代码块。
        }
        // 查询全部部门记录。
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> departments = maps(platformOrganizationRepository.listDepartments());
        // 声明本轮是否有新增部门。
        // 注释：执行当前代码行。
        boolean changed;
        // 循环扩展部门集合直到不再新增。
        // 注释：执行当前代码行。
        do {
            // 默认本轮没有新增部门。
            // 注释：设置或计算当前变量值。
            changed = false;
            // 遍历全部部门记录。
            // 注释：遍历当前数据集合。
            for (Map<String, Object> department : departments) {
                // 读取部门标识。
                // 注释：设置或计算当前变量值。
                String id = stringValue(department.get("id"));
                // 读取父部门标识。
                // 注释：设置或计算当前变量值。
                String parentId = stringValue(department.get("parentId"));
                // 父部门已允许且当前部门首次加入时标记发生变化。
                // 注释：判断当前业务条件。
                if (deptIds.contains(parentId) && deptIds.add(id)) {
                    // 标记本轮新增了部门。
                    // 注释：设置或计算当前变量值。
                    changed = true;
                    // 注释：结束当前代码块。
                }
                // 注释：结束当前代码块。
            }
            // 存在新增部门时继续下一轮扩展。
            // 注释：执行当前代码行。
        } while (changed);
        // 注释：结束当前代码块。
    }

    /**
     * 解析用户数据权限范围。
     *
     * @param userId 用户标识。
     * @return 数据权限范围编码。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String resolveDataScope(String userId) {
        // 默认管理员拥有全部数据权限。
        // 注释：判断当前业务条件。
        if (DEFAULT_ADMIN_USER_ID.equals(userId)) {
            // 返回全部数据权限编码。
            // 注释：返回当前处理结果。
            return DataScope.ALL.getCode();
            // 注释：结束当前代码块。
        }
        // 查询用户关联角色的数据权限编码列表。
        // 注释：设置或计算当前变量值。
        List<String> scopes = platformPermissionRepository.listDataScopesByUserId(userId);
        // 使用数据权限枚举解析最高权限范围。
        // 注释：返回当前处理结果。
        return DataScope.resolve(scopes).getCode();
        // 注释：结束当前代码块。
    }

    /**
     * 将对象转换为去空白字符串。
     *
     * @param value 原始值。
     * @return 去空白字符串。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 空值转换为空字符串，非空值转换为字符串并去除两侧空白。
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }

    /**
     * 将对象转换为整数。
     *
     * @param value 原始值。
     * @return 整数值。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private int intValue(Object value) {
        // 数字类型直接读取整数值。
        // 注释：判断当前业务条件。
        if (value instanceof Number) {
            // 返回数字对象的整数值。
            // 注释：返回当前处理结果。
            return ((Number) value).intValue();
            // 注释：结束当前代码块。
        }
        // 尝试按字符串解析整数。
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 返回字符串解析后的整数。
            // 注释：返回当前处理结果。
            return Integer.parseInt(stringValue(value));
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 解析失败时返回默认排序值。
            // 注释：返回当前处理结果。
            return 0;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 将对象转换为布尔值。
     *
     * @param value 原始值。
     * @param fieldName 字段名。
     * @return 布尔值。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private boolean booleanValue(Object value, String fieldName) {
        // 布尔类型直接返回。
        // 注释：判断当前业务条件。
        if (value instanceof Boolean) {
            // 返回原始布尔值。
            // 注释：返回当前处理结果。
            return (Boolean) value;
            // 注释：结束当前代码块。
        }
        // 将原始值转换为字符串。
        // 注释：设置或计算当前变量值。
        String text = stringValue(value);
        // true 或 false 字符串按忽略大小写方式解析。
        // 注释：判断当前业务条件。
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            // 返回字符串解析后的布尔值。
            // 注释：返回当前处理结果。
            return Boolean.parseBoolean(text);
            // 注释：结束当前代码块。
        }
        // 无法解析为布尔值时抛出参数异常。
        // 注释：抛出当前业务异常。
        throw new IllegalArgumentException(fieldName + " 必须是布尔值");
        // 注释：结束当前代码块。
    }

    /**
     * 将领域记录列表转换为字段 Map 列表。
     *
     * @param records 领域记录列表。
     * @return 字段 Map 列表。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 逐条导出领域记录的字段副本。
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 将领域记录 Optional 转换为字段 Map Optional。
     *
     * @param record 领域记录 Optional。
     * @return 字段 Map Optional。
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Optional<Map<String, Object>> optionalMap(Optional<DomainRecord> record) {
        // 在记录存在时导出字段副本。
        // 注释：返回当前处理结果。
        return record.map(DomainRecord::toMap);
        // 注释：结束当前代码块。
    }

    /**
     * 外观颜色模式枚举。
     */
    /**
     * 注释：当前枚举用于定义固定业务选项。
     */
    private enum AppearanceColorMode {

        /**
         * 浅色模式。
         */
        // 注释：执行当前代码行。
        LIGHT("light"),

        /**
         * 深色模式。
         */
        // 注释：执行当前代码行。
        DARK("dark"),

        /**
         * 跟随系统模式。
         */
        // 注释：执行当前代码行。
        SYSTEM("system");

        /**
         * 颜色模式编码。
         */
        // 注释：声明当前成员或方法。
        private final String code;

        /**
         * 绑定颜色模式编码。
         *
         * @param code 颜色模式编码。
         */
        // 注释：执行当前代码行。
        AppearanceColorMode(String code) {
            // 保存颜色模式编码。
            // 注释：设置或计算当前变量值。
            this.code = code;
            // 注释：结束当前代码块。
        }

        /**
         * 根据编码解析颜色模式。
         *
         * @param code 颜色模式编码。
         * @return 颜色模式枚举。
         */
        /**
         * 注释：当前方法用于执行对应业务处理。
         */
        private static Optional<AppearanceColorMode> fromCode(String code) {
            // 遍历全部颜色模式枚举。
            // 注释：遍历当前数据集合。
            for (AppearanceColorMode colorMode : values()) {
                // 命中颜色模式编码时返回枚举值。
                // 注释：判断当前业务条件。
                if (colorMode.code.equals(code)) {
                    // 返回匹配到的颜色模式枚举。
                    // 注释：返回当前处理结果。
                    return Optional.of(colorMode);
                    // 注释：结束当前代码块。
                }
                // 注释：结束当前代码块。
            }
            // 未命中颜色模式编码时返回空值。
            // 注释：返回当前处理结果。
            return Optional.empty();
            // 注释：结束当前代码块。
        }

        /**
         * 获取颜色模式编码。
         *
         * @return 颜色模式编码。
         */
        /**
         * 注释：当前方法用于执行对应业务处理。
         */
        private String getCode() {
            // 返回当前颜色模式编码。
            // 注释：返回当前处理结果。
            return code;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 外观视觉风格枚举。
     */
    /**
     * 注释：当前枚举用于定义固定业务选项。
     */
    private enum AppearanceVisualStyle {

        /**
         * 扁平风格。
         */
        // 注释：执行当前代码行。
        FLAT("flat"),

        /**
         * 玻璃风格。
         */
        // 注释：执行当前代码行。
        GLASS("glass");

        /**
         * 视觉风格编码。
         */
        // 注释：声明当前成员或方法。
        private final String code;

        /**
         * 绑定视觉风格编码。
         *
         * @param code 视觉风格编码。
         */
        // 注释：执行当前代码行。
        AppearanceVisualStyle(String code) {
            // 保存视觉风格编码。
            // 注释：设置或计算当前变量值。
            this.code = code;
            // 注释：结束当前代码块。
        }

        /**
         * 根据编码解析视觉风格。
         *
         * @param code 视觉风格编码。
         * @return 视觉风格枚举。
         */
        /**
         * 注释：当前方法用于执行对应业务处理。
         */
        private static Optional<AppearanceVisualStyle> fromCode(String code) {
            // 遍历全部视觉风格枚举。
            // 注释：遍历当前数据集合。
            for (AppearanceVisualStyle visualStyle : values()) {
                // 命中视觉风格编码时返回枚举值。
                // 注释：判断当前业务条件。
                if (visualStyle.code.equals(code)) {
                    // 返回匹配到的视觉风格枚举。
                    // 注释：返回当前处理结果。
                    return Optional.of(visualStyle);
                    // 注释：结束当前代码块。
                }
                // 注释：结束当前代码块。
            }
            // 未命中视觉风格编码时返回空值。
            // 注释：返回当前处理结果。
            return Optional.empty();
            // 注释：结束当前代码块。
        }

        /**
         * 获取视觉风格编码。
         *
         * @return 视觉风格编码。
         */
        /**
         * 注释：当前方法用于执行对应业务处理。
         */
        private String getCode() {
            // 返回当前视觉风格编码。
            // 注释：返回当前处理结果。
            return code;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
