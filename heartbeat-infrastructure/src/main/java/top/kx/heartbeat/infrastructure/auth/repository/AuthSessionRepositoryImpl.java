package top.kx.heartbeat.infrastructure.auth.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.auth.AuthSession;
import top.kx.heartbeat.domain.auth.AuthSessionRepository;
import top.kx.heartbeat.domain.auth.AuthSessionStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSessionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSessionDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSessionDOMapper;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Repository
public class AuthSessionRepositoryImpl implements AuthSessionRepository {

    @Resource
    private AuthSessionDOMapper authSessionMapper;

    @Override
    public Optional<AuthSession> findActive(long tenantId, String sessionId) {
        AuthSessionDOExample example = new AuthSessionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andSessionIdEqualTo(sessionId)
                .andStatusEqualTo(AuthSessionStatus.ACTIVE.getCode());
        return authSessionMapper.selectByExample(example)
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public AuthSession create(AuthSession session) {
        AuthSessionDO row = toRecord(session);
        row.setCreateTime(date(orNow(session.getCreateTime())));
        row.setUpdateTime(date(orNow(session.getUpdateTime())));
        authSessionMapper.insertSelective(row);
        return toDomain(row);
    }

    @Override
    public void rotateRefreshToken(long tenantId, String sessionId, String refreshTokenHash,
                                   LocalDateTime refreshExpireAt) {
        AuthSessionDO row = new AuthSessionDO();
        row.setRefreshTokenHash(refreshTokenHash);
        row.setRefreshExpireAt(date(refreshExpireAt));
        row.setUpdateTime(new Date());
        updateByTenantAndSession(tenantId, sessionId, row);
    }

    @Override
    public void touch(long tenantId, String sessionId, LocalDateTime lastAccessAt) {
        AuthSessionDO row = new AuthSessionDO();
        row.setLastAccessAt(date(lastAccessAt));
        row.setUpdateTime(new Date());
        updateByTenantAndSession(tenantId, sessionId, row);
    }

    @Override
    public void revoke(long tenantId, String sessionId, LocalDateTime revokedAt) {
        AuthSessionDO row = new AuthSessionDO();
        row.setStatus(AuthSessionStatus.REVOKED.getCode());
        row.setRevokedAt(date(revokedAt));
        row.setUpdateTime(new Date());
        updateByTenantAndSession(tenantId, sessionId, row);
    }

    private void updateByTenantAndSession(long tenantId, String sessionId, AuthSessionDO row) {
        AuthSessionDOExample example = new AuthSessionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId).andSessionIdEqualTo(sessionId);
        authSessionMapper.updateByExampleSelective(row, example);
    }

    private AuthSessionDO toRecord(AuthSession session) {
        AuthSessionDO row = new AuthSessionDO();
        row.setId(session.getId());
        row.setTenantId(session.getTenantId());
        row.setSessionId(session.getSessionId());
        row.setUserId(session.getUserId());
        row.setRefreshTokenHash(session.getRefreshTokenHash());
        row.setStatus(session.getStatus());
        row.setIssuedAt(date(session.getIssuedAt()));
        row.setExpireAt(date(session.getExpireAt()));
        row.setRefreshExpireAt(date(session.getRefreshExpireAt()));
        row.setRevokedAt(date(session.getRevokedAt()));
        row.setLastAccessAt(date(session.getLastAccessAt()));
        row.setCreateTime(date(session.getCreateTime()));
        row.setUpdateTime(date(session.getUpdateTime()));
        return row;
    }

    private AuthSession toDomain(AuthSessionDO row) {
        return AuthSession.builder()
                .id(row.getId())
                .tenantId(row.getTenantId() == null ? 0L : row.getTenantId())
                .sessionId(row.getSessionId())
                .userId(row.getUserId() == null ? 0L : row.getUserId())
                .refreshTokenHash(row.getRefreshTokenHash())
                .status(row.getStatus())
                .issuedAt(localDateTime(row.getIssuedAt()))
                .expireAt(localDateTime(row.getExpireAt()))
                .refreshExpireAt(localDateTime(row.getRefreshExpireAt()))
                .revokedAt(localDateTime(row.getRevokedAt()))
                .lastAccessAt(localDateTime(row.getLastAccessAt()))
                .createTime(localDateTime(row.getCreateTime()))
                .updateTime(localDateTime(row.getUpdateTime()))
                .build();
    }

    private LocalDateTime orNow(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }

    private Date date(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime localDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }
}
