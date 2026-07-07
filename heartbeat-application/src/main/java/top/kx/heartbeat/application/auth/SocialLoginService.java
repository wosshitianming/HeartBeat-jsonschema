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
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> providers = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map<String, Object> provider : maps(platformSocialRepository.listActiveSocialProviders())) {
            // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
            Map<String, Object> item = new LinkedHashMap<>();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("provider", firstValue(provider, "providerCode", "provider"));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("name", firstValue(provider, "providerName", "name"));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("icon", firstValue(provider, "icon"));
            // 加入当前处理结果，供后续批量返回或继续组装。
            providers.add(item);
        }
        // 返回已经完成封装的业务结果。
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
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::toMap)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String state = tokenIssuer.issueSocialState();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String callback = stringValue(firstValue(config, "redirectUri", "callbackUrl"));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String authorizeUrl = socialLoginHandlerRegistry.getRequired(provider)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .buildAuthorizeUrl(config, callback, state);

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("authorizeUrl", authorizeUrl);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("state", state);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("redirectAfterLogin", redirectAfterLogin);
        // 返回已经完成封装的业务结果。
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
        // 根据当前业务条件选择对应处理路径。
        if (!tokenIssuer.validateSocialState(state)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("state is invalid or expired");
        }
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> config = platformSocialRepository.findSocialProvider(provider)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::toMap)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, String> profile = socialLoginHandlerRegistry.getRequired(provider)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .resolveProfile(config, code);
        // 返回已经完成封装的业务结果。
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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, String> ticket = tokenIssuer.parseBindTicket(bindTicket);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> user = platformUserRepository.findUserByUsername(username)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::toMap)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        // 根据当前业务条件选择对应处理路径。
        if (!matchesPassword(password, stringValue(user.get("passwordHash")))) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            platformLoginLogRepository.recordLogin(username, "FAIL", "Social bind password error");
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Password is incorrect");
        }
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        bindSocial(user, ticket.get("provider"), ticket);
        // 返回已经完成封装的业务结果。
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
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            String provider,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            Map<String, Object> config,
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            Map<String, String> profile
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    ) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String openId = profile.get("openId");
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isBlank(openId)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Social profile openId must not be blank");
        }

        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Optional<Map<String, Object>> bindOptional = platformSocialRepository.findSocialBind(provider, openId)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::toMap);
        // 根据历史记录是否存在，选择更新或新增处理路径。
        if (bindOptional.isPresent()) {
            // 返回已经完成封装的业务结果。
            return loginResult(stringValue(bindOptional.get().get("userId")), provider);
        }

        // 根据当前业务条件选择对应处理路径。
        if (booleanValue(firstValue(config, "autoRegister"))) {
            // 创建下游写入请求对象，集中承载本次业务处理结果。
            PlatformUserRequest userRequest = new PlatformUserRequest();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            userRequest.setUsername(provider.toLowerCase() + "_" + openId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            userRequest.setNickname(profile.getOrDefault("nickname", provider + "用户"));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            userRequest.setAvatarUrl(profile.getOrDefault("avatar", ""));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            userRequest.setPasswordHash(openId);
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Map<String, Object> user = platformUserRepository.createSocialUser(userRequest).toMap();
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            bindSocial(user, provider, profile);
            // 返回已经完成封装的业务结果。
            return loginResult(stringValue(user.get("id")), provider);
        }

        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String bindTicket = tokenIssuer.issueBindTicket(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                provider,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                openId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                profile.getOrDefault("nickname", ""),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                profile.getOrDefault("avatar", "")
        );
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> pending = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        pending.put("status", "PENDING_BIND");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        pending.put("bindTicket", bindTicket);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        pending.put("provider", provider);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        pending.put("nickname", profile.getOrDefault("nickname", ""));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        pending.put("avatar", profile.getOrDefault("avatar", ""));
        // 返回已经完成封装的业务结果。
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
        // 创建下游写入请求对象，集中承载本次业务处理结果。
        PlatformSocialBindRequest request = new PlatformSocialBindRequest();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setUserId(stringValue(user.get("id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setProvider(provider);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setOpenId(profile.get("openId"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setUnionId(profile.get("unionId"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setNickname(profile.get("nickname"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        request.setAvatar(profile.get("avatar"));
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        Map<String, Object> user = platformUserRepository.findUserById(userId)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::toMap)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        platformLoginLogRepository.recordLogin(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                stringValue(user.get("username")),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "SUCCESS",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                provider + " login success"
        );
        // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
        AuthTokenResponse tokens = authenticationSessionService.createSession(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                userId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                stringValue(user.get("username")),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                stringValue(user.get("tenantId"))
        );
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("accessToken", tokens.getAccessToken());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("refreshToken", tokens.getRefreshToken());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("tokenType", tokens.getTokenType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("expiresIn", tokens.getExpiresIn());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("tenantId", tokens.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("sessionId", tokens.getSessionId());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        user.remove("passwordHash");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("user", user);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("permissions", platformPermissionRepository.listPermissionsByUserId(userId));
        // 返回已经完成封装的业务结果。
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
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String key : keys) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Object value = source.get(key);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (value != null) {
                // 返回已经完成封装的业务结果。
                return value;
            }
        }
        // 返回已经完成封装的业务结果。
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
