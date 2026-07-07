// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class SocialLoginService {

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
    private PlatformSocialRepository platformSocialRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformLoginLogRepository platformLoginLogRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private TokenIssuer tokenIssuer;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SocialLoginHandlerRegistry socialLoginHandlerRegistry;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthenticationSessionService authenticationSessionService;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private TransactionTemplate transactionTemplate;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listProvidersForLogin() {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> providers = new ArrayList<>();
        // 注释：遍历当前数据集合。
        for (Map<String, Object> provider : maps(platformSocialRepository.listActiveSocialProviders())) {
            // 注释：设置或计算当前变量值。
            Map<String, Object> item = new LinkedHashMap<>();
            // 注释：执行当前代码行。
            item.put("provider", firstValue(provider, "providerCode", "provider"));
            // 注释：执行当前代码行。
            item.put("name", firstValue(provider, "providerName", "name"));
            // 注释：执行当前代码行。
            item.put("icon", firstValue(provider, "icon"));
            // 注释：执行当前代码行。
            providers.add(item);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(providers);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse buildAuthorizeUrl(String provider, String redirectAfterLogin) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 注释：设置或计算当前变量值。
        String state = tokenIssuer.issueSocialState();
        // 注释：设置或计算当前变量值。
        String callback = stringValue(firstValue(config, "redirectUri", "callbackUrl"));
        // 注释：设置或计算当前变量值。
        String authorizeUrl = socialLoginHandlerRegistry.getRequired(provider)
                // 注释：继续当前链式调用。
                .buildAuthorizeUrl(config, callback, state);

        // 注释：设置或计算当前变量值。
        Map<String, Object> result = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        result.put("authorizeUrl", authorizeUrl);
        // 注释：执行当前代码行。
        result.put("state", state);
        // 注释：执行当前代码行。
        result.put("redirectAfterLogin", redirectAfterLogin);
        // 注释：返回当前处理结果。
        return RecordResponse.from(result);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse handleCallback(String provider, String code, String state) {
        // 注释：判断当前业务条件。
        if (!tokenIssuer.validateSocialState(state)) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("state is invalid or expired");
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 注释：设置或计算当前变量值。
        Map<String, String> profile = socialLoginHandlerRegistry.getRequired(provider)
                // 注释：继续当前链式调用。
                .resolveProfile(config, code);
        // 注释：返回当前处理结果。
        return transactionTemplate.execute(status -> handleResolvedProfile(provider, config, profile));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse bindExistingAccount(String bindTicket, String username, String password) {
        // 注释：设置或计算当前变量值。
        Map<String, String> ticket = tokenIssuer.parseBindTicket(bindTicket);
        // 注释：设置或计算当前变量值。
        Map<String, Object> user = platformUserRepository.findUserByUsername(username)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        // 注释：判断当前业务条件。
        if (!matchesPassword(password, stringValue(user.get("passwordHash")))) {
            // 注释：执行当前代码行。
            platformLoginLogRepository.recordLogin(username, "FAIL", "Social bind password error");
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Password is incorrect");
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        bindSocial(user, ticket.get("provider"), ticket);
        // 注释：返回当前处理结果。
        return loginResult(stringValue(user.get("id")), ticket.get("provider"));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private RecordResponse handleResolvedProfile(
            // 注释：执行当前代码行。
            String provider,
            // 注释：执行当前代码行。
            Map<String, Object> config,
            // 注释：执行当前代码行。
            Map<String, String> profile
            // 注释：结束当前多行调用。
    ) {
        // 注释：设置或计算当前变量值。
        String openId = profile.get("openId");
        // 注释：判断当前业务条件。
        if (StringUtils.isBlank(openId)) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Social profile openId must not be blank");
            // 注释：结束当前代码块。
        }

        // 注释：设置或计算当前变量值。
        Optional<Map<String, Object>> bindOptional = platformSocialRepository.findSocialBind(provider, openId)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap);
        // 注释：判断当前业务条件。
        if (bindOptional.isPresent()) {
            // 注释：返回当前处理结果。
            return loginResult(stringValue(bindOptional.get().get("userId")), provider);
            // 注释：结束当前代码块。
        }

        // 注释：判断当前业务条件。
        if (booleanValue(firstValue(config, "autoRegister"))) {
            // 注释：设置或计算当前变量值。
            PlatformUserRequest userRequest = new PlatformUserRequest();
            // 注释：执行当前代码行。
            userRequest.setUsername(provider.toLowerCase() + "_" + openId);
            // 注释：执行当前代码行。
            userRequest.setNickname(profile.getOrDefault("nickname", provider + "用户"));
            // 注释：执行当前代码行。
            userRequest.setAvatarUrl(profile.getOrDefault("avatar", ""));
            // 注释：执行当前代码行。
            userRequest.setPasswordHash(openId);
            // 注释：设置或计算当前变量值。
            Map<String, Object> user = platformUserRepository.createSocialUser(userRequest).toMap();
            // 注释：执行当前代码行。
            bindSocial(user, provider, profile);
            // 注释：返回当前处理结果。
            return loginResult(stringValue(user.get("id")), provider);
            // 注释：结束当前代码块。
        }

        // 注释：设置或计算当前变量值。
        String bindTicket = tokenIssuer.issueBindTicket(
                // 注释：执行当前代码行。
                provider,
                // 注释：执行当前代码行。
                openId,
                // 注释：执行当前代码行。
                profile.getOrDefault("nickname", ""),
                // 注释：执行当前代码行。
                profile.getOrDefault("avatar", "")
                // 注释：结束当前表达式。
        );
        // 注释：设置或计算当前变量值。
        Map<String, Object> pending = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        pending.put("status", "PENDING_BIND");
        // 注释：执行当前代码行。
        pending.put("bindTicket", bindTicket);
        // 注释：执行当前代码行。
        pending.put("provider", provider);
        // 注释：执行当前代码行。
        pending.put("nickname", profile.getOrDefault("nickname", ""));
        // 注释：执行当前代码行。
        pending.put("avatar", profile.getOrDefault("avatar", ""));
        // 注释：返回当前处理结果。
        return RecordResponse.from(pending);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void bindSocial(Map<String, Object> user, String provider, Map<String, String> profile) {
        // 注释：设置或计算当前变量值。
        PlatformSocialBindRequest request = new PlatformSocialBindRequest();
        // 注释：执行当前代码行。
        request.setUserId(stringValue(user.get("id")));
        // 注释：执行当前代码行。
        request.setProvider(provider);
        // 注释：执行当前代码行。
        request.setOpenId(profile.get("openId"));
        // 注释：执行当前代码行。
        request.setUnionId(profile.get("unionId"));
        // 注释：执行当前代码行。
        request.setNickname(profile.get("nickname"));
        // 注释：执行当前代码行。
        request.setAvatar(profile.get("avatar"));
        // 注释：执行当前代码行。
        platformSocialRepository.saveSocialBind(request);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private RecordResponse loginResult(String userId, String provider) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> user = platformUserRepository.findUserById(userId)
                // 注释：继续当前链式调用。
                .map(DomainRecord::toMap)
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        // 注释：执行当前代码行。
        platformLoginLogRepository.recordLogin(
                // 注释：执行当前代码行。
                stringValue(user.get("username")),
                // 注释：执行当前代码行。
                "SUCCESS",
                // 注释：执行当前代码行。
                provider + " login success"
                // 注释：结束当前表达式。
        );
        // 注释：设置或计算当前变量值。
        AuthTokenResponse tokens = authenticationSessionService.createSession(
                // 注释：执行当前代码行。
                userId,
                // 注释：执行当前代码行。
                stringValue(user.get("username")),
                // 注释：执行当前代码行。
                stringValue(user.get("tenantId"))
                // 注释：结束当前表达式。
        );
        // 注释：设置或计算当前变量值。
        Map<String, Object> result = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        result.put("accessToken", tokens.getAccessToken());
        // 注释：执行当前代码行。
        result.put("refreshToken", tokens.getRefreshToken());
        // 注释：执行当前代码行。
        result.put("tokenType", tokens.getTokenType());
        // 注释：执行当前代码行。
        result.put("expiresIn", tokens.getExpiresIn());
        // 注释：执行当前代码行。
        result.put("tenantId", tokens.getTenantId());
        // 注释：执行当前代码行。
        result.put("sessionId", tokens.getSessionId());
        // 注释：执行当前代码行。
        user.remove("passwordHash");
        // 注释：执行当前代码行。
        result.put("user", user);
        // 注释：执行当前代码行。
        result.put("permissions", platformPermissionRepository.listPermissionsByUserId(userId));
        // 注释：返回当前处理结果。
        return RecordResponse.from(result);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> result = new ArrayList<>();
        // 注释：遍历当前数据集合。
        for (DomainRecord record : records) {
            // 注释：执行当前代码行。
            result.add(record.toMap());
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return result;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private boolean matchesPassword(String rawPassword, String passwordHash) {
        // 注释：返回当前处理结果。
        return rawPassword.equals(passwordHash)
                // 注释：执行当前代码行。
                || rawPassword.equals("admin123") && "admin123".equals(passwordHash);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private boolean booleanValue(Object value) {
        // 注释：判断当前业务条件。
        if (value instanceof Boolean) {
            // 注释：返回当前处理结果。
            return (Boolean) value;
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return "true".equalsIgnoreCase(stringValue(value)) || "1".equals(stringValue(value));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Object firstValue(Map<String, Object> source, String... keys) {
        // 注释：遍历当前数据集合。
        for (String key : keys) {
            // 注释：设置或计算当前变量值。
            Object value = source.get(key);
            // 注释：判断当前业务条件。
            if (value != null) {
                // 注释：返回当前处理结果。
                return value;
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
