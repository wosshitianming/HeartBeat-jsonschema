package top.kx.heartbeat.application.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.port.PlatformLoginLogRepository;
import top.kx.heartbeat.application.platform.port.PlatformPermissionRepository;
import top.kx.heartbeat.application.platform.port.PlatformSocialRepository;
import top.kx.heartbeat.application.platform.port.PlatformUserRepository;
import top.kx.heartbeat.application.platform.request.PlatformSocialBindRequest;
import top.kx.heartbeat.application.platform.request.PlatformUserRequest;
import top.kx.heartbeat.domain.auth.SocialLoginHandlerRegistry;
import top.kx.heartbeat.domain.auth.TokenIssuer;

import javax.annotation.Resource;
import java.util.*;

/**
 * 编排认证登录应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class SocialLoginService {

    @Resource
    private PlatformUserRepository platformUserRepository;
    @Resource
    private PlatformPermissionRepository platformPermissionRepository;
    @Resource
    private PlatformSocialRepository platformSocialRepository;
    @Resource
    private PlatformLoginLogRepository platformLoginLogRepository;
    @Resource
    private TokenIssuer tokenIssuer;
    @Resource
    private SocialLoginHandlerRegistry socialLoginHandlerRegistry;
    @Resource
    private AuthenticationSessionService authenticationSessionService;
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调认证登录相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listProvidersForLogin() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (Map<String, Object> provider : maps(platformSocialRepository.listActiveSocialProviders())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("provider", firstValue(provider, "providerCode", "provider"));
            item.put("name", firstValue(provider, "providerName", "name"));
            item.put("icon", firstValue(provider, "icon"));
            providers.add(item);
        }
        return RecordResponse.fromMaps(providers);
    }

    /**
     * 组装业务处理所需的数据结构，降低主流程的理解成本，协调认证登录相关仓储和领域规则。
     *
     * @param provider 业务处理所需参数。
     * @param redirectAfterLogin 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    public RecordResponse buildAuthorizeUrl(String provider, String redirectAfterLogin) {
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                .map(DomainRecord::toMap)
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        String state = tokenIssuer.issueSocialState();
        String callback = stringValue(firstValue(config, "redirectUri", "callbackUrl"));
        String authorizeUrl = socialLoginHandlerRegistry.getRequired(provider)
                .buildAuthorizeUrl(config, callback, state);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authorizeUrl", authorizeUrl);
        result.put("state", state);
        result.put("redirectAfterLogin", redirectAfterLogin);
        return RecordResponse.from(result);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调认证登录相关仓储和领域规则。
     *
     * @param provider 业务处理所需参数。
     * @param code 业务处理所需参数。
     * @param state 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    public RecordResponse handleCallback(String provider, String code, String state) {
        if (!tokenIssuer.validateSocialState(state)) {
            throw new IllegalArgumentException("state is invalid or expired");
        }
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                .map(DomainRecord::toMap)
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        Map<String, String> profile = socialLoginHandlerRegistry.getRequired(provider)
                .resolveProfile(config, code);
        return transactionTemplate.execute(status -> handleResolvedProfile(provider, config, profile));
    }

    /**
     * 维护第三方账号绑定关系，保证登录身份可以正确关联本地用户，协调认证登录相关仓储和领域规则。
     *
     * @param bindTicket 业务处理所需参数。
     * @param username 登录用户名。
     * @param password 登录凭据。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse bindExistingAccount(String bindTicket, String username, String password) {
        Map<String, String> ticket = tokenIssuer.parseBindTicket(bindTicket);
        Map<String, Object> user = platformUserRepository.findUserByUsername(username)
                .map(DomainRecord::toMap)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (!matchesPassword(password, stringValue(user.get("passwordHash")))) {
            platformLoginLogRepository.recordLogin(username, "FAIL", "Social bind password error");
            throw new IllegalArgumentException("Password is incorrect");
        }
        bindSocial(user, ticket.get("provider"), ticket);
        return loginResult(stringValue(user.get("id")), ticket.get("provider"));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调认证登录相关仓储和领域规则。
     *
     * @param provider 业务处理所需参数。
     * @param config 业务处理所需参数。
     * @param profile 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private RecordResponse handleResolvedProfile(
            String provider,
            Map<String, Object> config,
            Map<String, String> profile
    ) {
        String openId = profile.get("openId");
        if (StringUtils.isBlank(openId)) {
            throw new IllegalArgumentException("Social profile openId must not be blank");
        }

        Optional<Map<String, Object>> bindOptional = platformSocialRepository.findSocialBind(provider, openId)
                .map(DomainRecord::toMap);
        if (bindOptional.isPresent()) {
            return loginResult(stringValue(bindOptional.get().get("userId")), provider);
        }

        if (booleanValue(firstValue(config, "autoRegister"))) {
            PlatformUserRequest userRequest = new PlatformUserRequest();
            userRequest.setUsername(provider.toLowerCase() + "_" + openId);
            userRequest.setNickname(profile.getOrDefault("nickname", provider + "用户"));
            userRequest.setAvatarUrl(profile.getOrDefault("avatar", ""));
            userRequest.setPasswordHash(openId);
            Map<String, Object> user = platformUserRepository.createSocialUser(userRequest).toMap();
            bindSocial(user, provider, profile);
            return loginResult(stringValue(user.get("id")), provider);
        }

        String bindTicket = tokenIssuer.issueBindTicket(
                provider,
                openId,
                profile.getOrDefault("nickname", ""),
                profile.getOrDefault("avatar", "")
        );
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("status", "PENDING_BIND");
        pending.put("bindTicket", bindTicket);
        pending.put("provider", provider);
        pending.put("nickname", profile.getOrDefault("nickname", ""));
        pending.put("avatar", profile.getOrDefault("avatar", ""));
        return RecordResponse.from(pending);
    }

    /**
     * 维护第三方账号绑定关系，保证登录身份可以正确关联本地用户，协调认证登录相关仓储和领域规则。
     *
     * @param user 业务处理所需参数。
     * @param provider 业务处理所需参数。
     * @param profile 业务处理所需参数。
     */
    private void bindSocial(Map<String, Object> user, String provider, Map<String, String> profile) {
        PlatformSocialBindRequest request = new PlatformSocialBindRequest();
        request.setUserId(stringValue(user.get("id")));
        request.setProvider(provider);
        request.setOpenId(profile.get("openId"));
        request.setUnionId(profile.get("unionId"));
        request.setNickname(profile.get("nickname"));
        request.setAvatar(profile.get("avatar"));
        platformSocialRepository.saveSocialBind(request);
    }

    /**
     * 完成登录校验并组装前端需要的登录态信息，协调认证登录相关仓储和领域规则。
     *
     * @param userId 业务记录标识。
     * @param provider 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private RecordResponse loginResult(String userId, String provider) {
        Map<String, Object> user = platformUserRepository.findUserById(userId)
                .map(DomainRecord::toMap)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        platformLoginLogRepository.recordLogin(
                stringValue(user.get("username")),
                "SUCCESS",
                provider + " login success"
        );
        AuthTokenResponse tokens = authenticationSessionService.createSession(
                userId,
                stringValue(user.get("username")),
                stringValue(user.get("tenantId"))
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", tokens.getAccessToken());
        result.put("refreshToken", tokens.getRefreshToken());
        result.put("tokenType", tokens.getTokenType());
        result.put("expiresIn", tokens.getExpiresIn());
        result.put("tenantId", tokens.getTenantId());
        result.put("sessionId", tokens.getSessionId());
        user.remove("passwordHash");
        result.put("user", user);
        result.put("permissions", platformPermissionRepository.listPermissionsByUserId(userId));
        return RecordResponse.from(result);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调认证登录相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DomainRecord record : records) {
            result.add(record.toMap());
        }
        return result;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调认证登录相关仓储和领域规则。
     *
     * @param rawPassword 业务处理所需参数。
     * @param passwordHash 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private boolean matchesPassword(String rawPassword, String passwordHash) {
        return rawPassword.equals(passwordHash)
                || rawPassword.equals("admin123") && "admin123".equals(passwordHash);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调认证登录相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(stringValue(value)) || "1".equals(stringValue(value));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调认证登录相关仓储和领域规则。
     *
     * @param source 业务处理所需参数。
     * @param keys 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Object firstValue(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，协调认证登录相关仓储和领域规则。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
