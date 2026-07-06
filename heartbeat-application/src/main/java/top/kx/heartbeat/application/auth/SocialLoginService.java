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

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DomainRecord record : records) {
            result.add(record.toMap());
        }
        return result;
    }

    private boolean matchesPassword(String rawPassword, String passwordHash) {
        return rawPassword.equals(passwordHash)
                || rawPassword.equals("admin123") && "admin123".equals(passwordHash);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(stringValue(value)) || "1".equals(stringValue(value));
    }

    private Object firstValue(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
