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
 * 提供认证登录 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Resource
    private PlatformAdministrationService adminPlatformService;

    @Resource
    private SocialLoginService socialLoginService;

    @Resource
    private AuthenticationSessionService authenticationSessionService;

    /**
     * 完成登录校验并组装前端需要的登录态信息，并统一委托认证登录应用服务完成处理。
     *
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 调用平台服务完成认证并签发令牌。
        // 返回登录成功响应。
        return Result.success(adminPlatformService.login(request.toPlatformRequest()));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param userId 业务记录标识。
     * @param authorization 业务处理所需参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param userId 业务记录标识。
     * @param authorization 业务处理所需参数。
     * @return 处理后的业务结果。
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
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托认证登录应用服务完成处理。
     *
     * @param userId 业务记录标识。
     * @param authorization 业务处理所需参数。
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
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
     * 清理登录态并结束当前会话，并统一委托认证登录应用服务完成处理。
     *
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param provider 业务处理所需参数。
     * @param redirect 业务处理所需参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param provider 业务处理所需参数。
     * @param code 业务处理所需参数。
     * @param state 业务处理所需参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param provider 业务处理所需参数。
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托认证登录应用服务完成处理。
     *
     * @param request 认证登录请求参数。
     * @return 处理后的业务结果。
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
