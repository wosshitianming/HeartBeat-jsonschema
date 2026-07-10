package top.kx.heartbeat.application.auth;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.auth.response.AuthTokenResponse;
import top.kx.heartbeat.domain.auth.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthenticationSessionService {

    private static final long SESSION_TOUCH_INTERVAL_MINUTES = 5L;

    @Resource
    private AuthSessionRepository authSessionRepository;
    @Resource
    private TokenIssuer tokenIssuer;

    @Transactional
    public AuthTokenResponse createSession(String userId, String username, String tenantId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        long uid = positiveLong(userId, "userId");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        long tid = positiveLong(tenantId, "tenantId");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String sessionId = UUID.randomUUID().toString();
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        AuthTokenPayload tokens = tokenIssuer.issueTokens(String.valueOf(uid), username, String.valueOf(tid), sessionId);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        LocalDateTime now = LocalDateTime.now();
        // 追加代码或文本片段，逐步生成最终内容。
        authSessionRepository.create(AuthSession.builder()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .tenantId(tid)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .sessionId(sessionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .userId(uid)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .refreshTokenHash(sha256(tokens.getRefreshToken()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .status(AuthSessionStatus.ACTIVE.getCode())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .issuedAt(now)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .expireAt(now.plusSeconds(tokenIssuer.accessTokenTtlSeconds()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .refreshExpireAt(now.plusSeconds(tokenIssuer.refreshTokenTtlSeconds()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .lastAccessAt(now)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .createTime(now)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .updateTime(now)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .build());
        // 返回已经完成封装的业务结果。
        return AuthTokenResponse.from(tokens);
    }

    @Transactional
    public AuthSession requireActive(long tenantId, String userId, String sessionId) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        long uid = positiveLong(userId, "userId");
        // 计算当前分支的中间结果，供后续判断或组装使用。
        LocalDateTime now = LocalDateTime.now();
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        AuthSession session = authSessionRepository.findActive(tenantId, sessionId)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        // 根据当前业务条件选择对应处理路径。
        if (session.getUserId() != uid || !session.isActiveAt(now)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Auth session is not active");
        }
        // 限制最近访问时间的写入频率，避免每次鉴权都更新同一会话记录。
        if (shouldTouch(session, now)) {
            authSessionRepository.touch(tenantId, sessionId, now);
        }
        // 返回已经完成封装的业务结果。
        return session;
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
        AuthTokenClaims claims = tokenIssuer.parseRefreshToken(refreshToken);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        LocalDateTime now = LocalDateTime.now();
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        AuthSession session = authSessionRepository.findActive(claims.getTenantId(), claims.getSessionId())
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        // 根据当前业务条件选择对应处理路径。
        if (session.getUserId() != positiveLong(claims.getUserId(), "userId") || !session.canRefreshAt(now)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Auth session cannot be refreshed");
        }
        String currentRefreshTokenHash = sha256(refreshToken);
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!currentRefreshTokenHash.equals(session.getRefreshTokenHash())) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Refresh token has been rotated");
        }

        // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
        AuthTokenPayload tokens = tokenIssuer.issueTokens(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                claims.getUserId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                claims.getUsername(),
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                String.valueOf(claims.getTenantId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                claims.getSessionId()
        );
        // 只有数据库中的旧令牌哈希仍匹配时才完成轮换，阻止并发刷新和重放。
        boolean rotated = authSessionRepository.rotateRefreshToken(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                claims.getTenantId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                claims.getSessionId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                currentRefreshTokenHash,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                sha256(tokens.getRefreshToken()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                now.plusSeconds(tokenIssuer.refreshTokenTtlSeconds())
        );
        if (!rotated) {
            throw new IllegalArgumentException("Refresh token has been rotated");
        }
        // 返回已经完成封装的业务结果。
        return AuthTokenResponse.from(tokens);
    }

    @Transactional
    public void logout(long tenantId, String sessionId) {
        if (tenantId <= 0 || !hasText(sessionId)) {
            return;
        }
        authSessionRepository.revoke(tenantId, sessionId, LocalDateTime.now());
    }

    private long positiveLong(String value, String fieldName) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            long result = Long.parseLong(stringValue(value));
            // 根据当前业务条件选择对应处理路径。
            if (result > 0) {
                // 返回已经完成封装的业务结果。
                return result;
            }
        } catch (NumberFormatException ignored) {
        }
        // 对非法业务状态立即失败，避免错误继续扩散。
        throw new IllegalArgumentException(fieldName + " must be a positive long");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }

    private boolean shouldTouch(AuthSession session, LocalDateTime now) {
        LocalDateTime lastAccessAt = session.getLastAccessAt();
        return lastAccessAt == null
                || !lastAccessAt.plusMinutes(SESSION_TOUCH_INTERVAL_MINUTES).isAfter(now);
    }

    private String sha256(String value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            byte[] bytes = digest.digest(stringValue(value).getBytes(StandardCharsets.UTF_8));
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            StringBuilder builder = new StringBuilder();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (byte item : bytes) {
                // 按签名算法处理字节数据，保证验签结果可重复计算。
                builder.append(String.format("%02x", item));
            }
            // 返回已经完成封装的业务结果。
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
