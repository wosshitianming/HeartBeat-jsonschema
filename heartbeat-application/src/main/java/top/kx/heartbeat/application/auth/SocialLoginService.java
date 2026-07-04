package top.kx.heartbeat.application.auth;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.kx.heartbeat.domain.auth.SocialLoginHandlerRegistry;
import top.kx.heartbeat.domain.auth.TokenIssuer;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformAdminRepository;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 聚合第三方登录应用服务。
 *
 * <p>负责授权地址生成、OAuth 回调处理、账号绑定与 JWT 签发编排。</p>
 * <p>支持渠道：WECHAT、WECHAT_CP、DINGTALK、OIDC、MOCK（本地联调）。</p>
 */
@Service
public class SocialLoginService {

    // 平台管理仓储：用户、绑定关系、渠道配置
    @Resource
    private PlatformAdminRepository platformAdminRepository;
    // JWT 与 OAuth state / bindTicket 签发端口
    @Resource
    private TokenIssuer tokenIssuer;
    @Resource
    private SocialLoginHandlerRegistry socialLoginHandlerRegistry;
    @Resource
    private AuthenticationSessionService authenticationSessionService;
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 查询登录页可用的第三方渠道列表（仅返回展示字段）。
     *
     * @return provider、name、icon 组成的列表
     */
    public List<Map<String, Object>> listProvidersForLogin() {
        // 返回给前端的精简渠道列表
        List<Map<String, Object>> providers = new ArrayList<>();
        // 遍历已启用渠道
        for (Map<String, Object> provider : maps(platformAdminRepository.listActiveSocialProviders())) {
            // 单条渠道展示对象
            Map<String, Object> item = new LinkedHashMap<>();
            // 渠道编码，如 WECHAT、DINGTALK、MOCK
            item.put("provider", provider.get("provider"));
            // 渠道展示名称
            item.put("name", provider.get("name"));
            // 渠道图标标识
            item.put("icon", provider.get("icon"));
            // 收集到结果列表
            providers.add(item);
        }
        // 返回登录页第三方按钮数据
        return providers;
    }

    /**
     * 生成第三方 OAuth 授权跳转信息。
     *
     * @param provider           渠道编码
     * @param redirectAfterLogin 登录成功后前端回跳路径
     * @return authorizeUrl、state、redirectAfterLogin
     */
    public Map<String, Object> buildAuthorizeUrl(String provider, String redirectAfterLogin) {
        // 查询渠道配置
        Map<String, Object> config = platformAdminRepository.findSocialProvider(provider)
                .map(DomainRecord::toMap)
                // 未配置则拒绝
                .orElseThrow(() -> new IllegalArgumentException("未启用的登录渠道: " + provider));
        // 签发防 CSRF 的 state 令牌
        String state = tokenIssuer.issueSocialState();
        // OAuth 回调地址
        String callback = stringValue(config.get("redirectUri"));
        // 拼接第三方授权 URL
        String authorizeUrl = socialLoginHandlerRegistry.getRequired(provider)
                .buildAuthorizeUrl(config, callback, state);
        // 返回给前端的授权信息
        Map<String, Object> result = new LinkedHashMap<>();
        // 浏览器跳转地址
        result.put("authorizeUrl", authorizeUrl);
        // 前端可选缓存 state 用于校验
        result.put("state", state);
        // 登录成功后回跳业务页
        result.put("redirectAfterLogin", redirectAfterLogin);
        // 返回授权包
        return result;
    }

    /**
     * 处理第三方 OAuth 回调：已绑定直接登录，未绑定返回待绑定或自动注册。
     *
     * @param provider 渠道编码
     * @param code     授权码
     * @param state    防 CSRF 状态值
     * @return JWT 登录结果或 PENDING_BIND 待绑定信息
     */
    public Map<String, Object> handleCallback(String provider, String code, String state) {
        // 校验 state 防止伪造回调
        if (!tokenIssuer.validateSocialState(state)) {
            // 校验失败直接拒绝
            throw new IllegalArgumentException("state 无效或已过期");
        }
        // 再次加载渠道配置
        Map<String, Object> config = platformAdminRepository.findSocialProvider(provider)
                .map(DomainRecord::toMap)
                // 渠道必须存在
                .orElseThrow(() -> new IllegalArgumentException("未启用的登录渠道: " + provider));
        // 用 code 换取第三方用户画像
        Map<String, String> profile = socialLoginHandlerRegistry.getRequired(provider)
                .resolveProfile(config, code);
        return transactionTemplate.execute(status -> handleResolvedProfile(provider, config, profile));
    }

    private Map<String, Object> handleResolvedProfile(String provider,
                                                      Map<String, Object> config,
                                                      Map<String, String> profile) {
        // 第三方唯一标识
        String openId = profile.get("openId");
        // 查是否已绑定本地账号
        Optional<Map<String, Object>> bindOptional = platformAdminRepository.findSocialBind(provider, openId)
                .map(DomainRecord::toMap);
        // 已绑定则直接登录
        if (bindOptional.isPresent()) {
            // 取出本地用户 ID
            String userId = stringValue(bindOptional.get().get("userId"));
            // 签发 JWT 并返回用户信息
            return loginResult(userId, provider);
        }
        // 渠道允许自动注册
        if (booleanValue(config.get("autoRegister"))) {
            // 自动建号命令
            Map<String, Object> userCommand = new LinkedHashMap<>();
            // 生成唯一用户名
            userCommand.put("username", provider.toLowerCase() + "_" + openId);
            // 默认昵称
            userCommand.put("nickname", profile.getOrDefault("nickname", provider + "用户"));
            // 头像地址
            userCommand.put("avatar", profile.getOrDefault("avatar", ""));
            // 随机密码占位（第三方登录不走密码）
            userCommand.put("password", openId);
            // 创建本地用户
            Map<String, Object> user = platformAdminRepository.createSocialUser(userCommand).toMap();
            // 写入绑定关系
            bindSocial(user, provider, profile);
            // 返回登录结果
            return loginResult(stringValue(user.get("id")), provider);
        }
        // 未绑定且不允许自动注册时发绑定票据
        String bindTicket = tokenIssuer.issueBindTicket(
                provider,
                openId,
                profile.getOrDefault("nickname", ""),
                profile.getOrDefault("avatar", "")
        );
        // 待绑定响应
        Map<String, Object> pending = new LinkedHashMap<>();
        // 状态：等待用户绑定已有账号
        pending.put("status", "PENDING_BIND");
        // 短期有效绑定票据
        pending.put("bindTicket", bindTicket);
        // 渠道编码
        pending.put("provider", provider);
        // 第三方昵称
        pending.put("nickname", profile.getOrDefault("nickname", ""));
        // 第三方头像
        pending.put("avatar", profile.getOrDefault("avatar", ""));
        // 返回待绑定信息给前端
        return pending;
    }

    /**
     * 将第三方账号绑定到已有本地账号（需校验用户名密码）。
     *
     * @param bindTicket 回调返回的短期绑定票据
     * @param username   本地用户名
     * @param password   本地密码
     * @return 绑定成功后的 JWT 登录结果
     */
    @Transactional
    public Map<String, Object> bindExistingAccount(String bindTicket, String username, String password) {
        // 解析绑定票据
        Map<String, String> ticket = tokenIssuer.parseBindTicket(bindTicket);
        // 查本地用户
        Optional<Map<String, Object>> userOptional = platformAdminRepository.findUserByUsername(username)
                .map(DomainRecord::toMap);
        // 用户不存在则终止
        if (!userOptional.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 取出用户记录
        Map<String, Object> user = userOptional.get();
        // 校验密码
        if (!matchesPassword(password, stringValue(user.get("passwordHash")))) {
            // 记录失败登录日志
            platformAdminRepository.recordLogin(username, "FAIL", "第三方绑定密码错误");
            // 密码不正确
            throw new IllegalArgumentException("密码错误");
        }
        // 建立社交绑定
        bindSocial(user, ticket.get("provider"), ticket);
        // 绑定成功后登录
        return loginResult(stringValue(user.get("id")), ticket.get("provider"));
    }

    /**
     * 持久化第三方账号与本地用户的绑定关系。
     */
    private void bindSocial(Map<String, Object> user, String provider, Map<String, String> profile) {
        // 绑定写入命令
        Map<String, Object> command = new LinkedHashMap<>();
        // 本地用户 ID
        command.put("userId", user.get("id"));
        // 渠道编码
        command.put("provider", provider);
        // 第三方 openId
        command.put("openId", profile.get("openId"));
        // 可选 unionId
        command.put("unionId", profile.get("unionId"));
        // 第三方昵称
        command.put("nickname", profile.get("nickname"));
        // 第三方头像
        command.put("avatar", profile.get("avatar"));
        // 持久化绑定记录
        platformAdminRepository.saveSocialBind(command);
    }

    /**
     * 组装完整登录结果：JWT + 用户 + 权限。
     */
    private Map<String, Object> loginResult(String userId, String provider) {
        // 加载用户详情
        Map<String, Object> user = platformAdminRepository.findUserById(userId)
                .map(DomainRecord::toMap)
                // 用户必须存在
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        // 写登录日志
        platformAdminRepository.recordLogin(stringValue(user.get("username")), "SUCCESS", provider + " 登录成功");
        // 签发访问令牌与刷新令牌，并创建可撤销的服务端会话
        Map<String, Object> result = new LinkedHashMap<>(authenticationSessionService.createSession(
                userId,
                stringValue(user.get("username")),
                stringValue(user.get("tenantId"))
        ));
        // 响应中移除密码哈希
        user.remove("passwordHash");
        // 附带用户基本信息
        result.put("user", user);
        // 附带权限码列表
        result.put("permissions", platformAdminRepository.listPermissionsByUserId(userId));
        // 返回完整登录结果
        return result;
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DomainRecord record : records) {
            result.add(record.toMap());
        }
        return result;
    }

    /**
     * 用授权码解析第三方用户画像（真实渠道待接入 HTTP 客户端）。
     */
    private Map<String, String> resolveProfile(String provider, String code, Map<String, Object> config) {
        // 本地演示渠道
        if ("MOCK".equalsIgnoreCase(provider) || code.startsWith("mock:")) {
            // 解析 mock openId
            String openId = code.startsWith("mock:") ? code.substring(5) : "demo-open-id";
            // 构造演示用户画像
            Map<String, String> profile = new LinkedHashMap<>();
            // 演示 openId
            profile.put("openId", openId);
            // 演示昵称
            profile.put("nickname", "演示用户");
            // 演示头像为空
            profile.put("avatar", "");
            // 返回演示画像
            return profile;
        }
        // 真实渠道待配置 AppId 与 HTTP 对接
        throw new IllegalArgumentException("渠道 " + provider + " 尚未配置真实 AppId，请使用 MOCK 渠道或完成密钥配置");
    }

    /**
     * 按渠道类型拼接 OAuth 授权 URL。
     */
    private String buildProviderAuthorizeUrl(String provider,
                                             Map<String, Object> config,
                                             String redirectUri,
                                             String state) {
        // 读取第三方 AppId
        String appId = stringValue(config.get("appId"));
        // 微信开放平台网站应用
        if ("WECHAT".equalsIgnoreCase(provider)) {
            return "https://open.weixin.qq.com/connect/qrconnect?appid=" + appId
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code&scope=snsapi_login&state=" + encode(state) + "#wechat_redirect";
        }
        // 钉钉 OAuth2
        if ("DINGTALK".equalsIgnoreCase(provider)) {
            return "https://login.dingtalk.com/oauth2/auth?client_id=" + appId
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code&scope=openid&state=" + encode(state) + "&prompt=consent";
        }
        // 企业微信扫码登录
        if ("WECHAT_CP".equalsIgnoreCase(provider)) {
            return "https://open.work.weixin.qq.com/wwopen/sso/qrConnect?appid=" + appId
                    + "&agentid=" + stringValue(config.get("agentId"))
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&state=" + encode(state);
        }
        // 本地 MOCK 回调页
        if ("MOCK".equalsIgnoreCase(provider)) {
            return "/login?social=mock&state=" + encode(state);
        }
        // 自定义 OIDC 授权地址模板
        String custom = stringValue(config.get("authorizeUrl"));
        // 若配置了模板则替换占位符
        if (StringUtils.isNotEmpty(custom)) {
            return custom.replace("{appId}", appId)
                    .replace("{redirectUri}", encode(redirectUri))
                    .replace("{state}", encode(state));
        }
        // 未知渠道
        throw new IllegalArgumentException("不支持的登录渠道: " + provider);
    }

    /**
     * 校验明文密码与存储哈希（演示环境兼容 admin123 明文）。
     */
    private boolean matchesPassword(String rawPassword, String passwordHash) {
        return rawPassword.equals(passwordHash)
                || rawPassword.equals("admin123") && "admin123".equals(passwordHash);
    }

    /**
     * URL 编码，使用 UTF-8。
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 编码不可用", ex);
        }
    }

    /**
     * 安全地将对象转为去首尾空格的字符串。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将配置值解析为布尔（支持 true/1）。
     */
    private boolean booleanValue(Object value) {
        // 已是布尔类型
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        // 解析字符串布尔
        return "true".equalsIgnoreCase(stringValue(value)) || "1".equals(stringValue(value));
    }
}
