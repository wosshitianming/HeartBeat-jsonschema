package top.kx.heartbeat.interfaces.auth;


import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.auth.AuthenticationSessionService;
import top.kx.heartbeat.application.auth.SocialLoginService;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.application.platform.response.LoginResponse;
import top.kx.heartbeat.interfaces.auth.request.*;
import top.kx.heartbeat.interfaces.auth.response.SocialProviderResponse;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 认证中心接口控制器。
 *
 * <p>负责账号密码登录、令牌刷新、当前用户、外观偏好和第三方登录的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * 平台认证与用户查询服务。
     */
    @Resource
    private PlatformAdministrationService adminPlatformService;

    /**
     * 第三方登录编排服务。
     */
    @Resource
    private SocialLoginService socialLoginService;

    /**
     * 服务端认证会话生命周期服务。
     */
    @Resource
    private AuthenticationSessionService authenticationSessionService;

    /**
     * 执行账号密码登录。
     *
     * @param request 账号密码登录请求对象
     * @return 登录令牌与用户上下文响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 调用平台服务完成认证并签发令牌。
        // 返回登录成功响应。
        return Result.success(adminPlatformService.login(request.toPlatformRequest()));
    }

    /**
     * 查询当前登录用户信息。
     *
     * @param userId 当前用户标识请求头
     * @param authorization 授权令牌请求头
     * @return 当前用户上下文响应
     */
    @GetMapping("/me")
    public Result<DynamicRecordResponse> me(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 查询当前用户聚合信息。
        RecordResponse currentUser = adminPlatformService.currentUser();
        // 将动态用户上下文转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(currentUser);
        // 返回当前用户上下文。
        return Result.success(response);
    }

    /**
     * 读取当前用户外观偏好。
     *
     * @param userId 当前用户标识请求头
     * @param authorization 授权令牌请求头
     * @return 当前用户外观偏好响应
     */
    @GetMapping("/preferences/appearance")
    public Result<DynamicRecordResponse> appearancePreference(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 读取当前用户外观偏好。
        RecordResponse preference = adminPlatformService.appearancePreference();
        // 将动态偏好配置转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(preference);
        // 返回当前用户外观偏好。
        return Result.success(response);
    }

    /**
     * 更新当前用户外观偏好。
     *
     * @param userId 当前用户标识请求头
     * @param authorization 授权令牌请求头
     * @param request 外观偏好保存请求对象
     * @return 当前用户外观偏好保存响应
     */
    @PutMapping("/preferences/appearance")
    public Result<DynamicRecordResponse> updateAppearancePreference(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AppearancePreferenceRequest request) {
        // 持久化当前用户外观偏好。
        RecordResponse saved = adminPlatformService.updateAppearancePreference(request.toPlatformRequest());
        // 将动态偏好配置转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(saved);
        // 返回当前用户外观偏好保存结果。
        return Result.success(response);
    }

    /**
     * 使用刷新令牌换取新的访问令牌。
     *
     * @param request 刷新令牌请求对象
     * @return 新令牌响应
     */
    @PostMapping("/refresh")
    public Result<AuthTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        // 读取刷新令牌。
        String refreshToken = request.getRefreshToken();
        // 校验服务端会话并轮换刷新令牌。
        // 返回新令牌响应。
        return Result.success(authenticationSessionService.refresh(refreshToken));
    }

    /**
     * 退出当前登录会话。
     *
     * @param request HTTP 请求上下文
     * @return 退出登录结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 读取租户标识上下文。
        Object tenantId = request.getAttribute("heartbeatTenantId");
        // 读取会话标识上下文。
        Object sessionId = request.getAttribute("heartbeatSessionId");
        // 判断上下文是否具备服务端会话信息。
        if (tenantId != null && sessionId != null) {
            // 撤销当前服务端认证会话。
            authenticationSessionService.logout(Long.parseLong(String.valueOf(tenantId)), String.valueOf(sessionId));
        }
        // 返回退出成功响应。
        return Result.success();
    }

    /**
     * 查询登录页可用第三方登录渠道。
     *
     * @return 第三方登录渠道列表响应
     */
    @GetMapping("/social/providers")
    public Result<List<SocialProviderResponse>> socialProviders() {
        // 查询登录页精简渠道列表。
        List<RecordResponse> providers = socialLoginService.listProvidersForLogin();
        // 转换为第三方登录渠道响应对象列表。
        List<SocialProviderResponse> response = SocialProviderResponse.fromRecordList(providers);
        // 返回第三方登录渠道列表。
        return Result.success(response);
    }

    /**
     * 生成第三方 OAuth 授权跳转信息。
     *
     * @param provider 第三方登录渠道编码
     * @param redirect 登录成功后的前端回跳地址
     * @return 第三方授权跳转响应
     */
    @GetMapping("/social/{provider}/authorize")
    public Result<DynamicRecordResponse> socialAuthorize(
            @PathVariable String provider,
            @RequestParam(value = "redirect", required = false, defaultValue = "/admin") String redirect) {
        // 生成第三方授权地址与状态值。
        RecordResponse authorize = socialLoginService.buildAuthorizeUrl(provider, redirect);
        // 将动态授权信息转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(authorize);
        // 返回第三方授权跳转信息。
        return Result.success(response);
    }

    /**
     * 处理第三方 OAuth 浏览器回调。
     *
     * @param provider 第三方登录渠道编码
     * @param code OAuth 授权码
     * @param state 防重放状态值
     * @return 第三方回调处理响应
     */
    @GetMapping("/social/{provider}/callback")
    public Result<DynamicRecordResponse> socialCallbackGet(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        // 处理第三方 OAuth 回调。
        RecordResponse callbackResult = socialLoginService.handleCallback(provider, code, state);
        // 将动态回调结果转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(callbackResult);
        // 返回第三方回调处理结果。
        return Result.success(response);
    }

    /**
     * 处理第三方 OAuth 前端提交回调。
     *
     * @param provider 第三方登录渠道编码
     * @param request 第三方回调请求对象
     * @return 第三方回调处理响应
     */
    @PostMapping("/social/{provider}/callback")
    public Result<DynamicRecordResponse> socialCallbackPost(
            @PathVariable String provider,
            @RequestBody SocialCallbackRequest request) {
        // 读取 OAuth 授权码。
        String code = request.getCode();
        // 读取防重放状态值。
        String state = request.getState();
        // 处理第三方 OAuth 回调。
        RecordResponse callbackResult = socialLoginService.handleCallback(provider, code, state);
        // 将动态回调结果转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(callbackResult);
        // 返回第三方回调处理结果。
        return Result.success(response);
    }

    /**
     * 绑定第三方账号到已有本地账号。
     *
     * @param request 第三方账号绑定请求对象
     * @return 第三方账号绑定登录响应
     */
    @PostMapping("/social/bind")
    public Result<DynamicRecordResponse> socialBind(@RequestBody SocialBindRequest request) {
        // 读取绑定票据。
        String bindTicket = request.getBindTicket();
        // 读取本地用户名。
        String username = request.getUsername();
        // 读取本地密码。
        String password = request.getPassword();
        // 校验本地账号并建立第三方绑定关系。
        RecordResponse bindResult = socialLoginService.bindExistingAccount(bindTicket, username, password);
        // 将动态绑定结果转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(bindResult);
        // 返回第三方账号绑定登录结果。
        return Result.success(response);
    }
}
