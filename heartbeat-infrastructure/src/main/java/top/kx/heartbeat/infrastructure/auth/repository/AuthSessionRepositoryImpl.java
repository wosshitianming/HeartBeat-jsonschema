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
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        AuthSessionDOExample example = new AuthSessionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andTenantIdEqualTo(tenantId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andSessionIdEqualTo(sessionId)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .andStatusEqualTo(AuthSessionStatus.ACTIVE.getCode());
        // 返回已经完成封装的业务结果。
        return authSessionMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .findFirst()
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
                                   // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                   LocalDateTime refreshExpireAt) {
        // 创建数据库记录对象，承载即将写入的业务字段。
        AuthSessionDO row = new AuthSessionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRefreshTokenHash(refreshTokenHash);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRefreshExpireAt(date(refreshExpireAt));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(new Date());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 创建数据库记录对象，承载即将写入的业务字段。
        AuthSessionDO row = new AuthSessionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(AuthSessionStatus.REVOKED.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRevokedAt(date(revokedAt));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(new Date());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        updateByTenantAndSession(tenantId, sessionId, row);
    }

    private void updateByTenantAndSession(long tenantId, String sessionId, AuthSessionDO row) {
        AuthSessionDOExample example = new AuthSessionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId).andSessionIdEqualTo(sessionId);
        authSessionMapper.updateByExampleSelective(row, example);
    }

    private AuthSessionDO toRecord(AuthSession session) {
        // 创建数据库记录对象，承载即将写入的业务字段。
        AuthSessionDO row = new AuthSessionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setId(session.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(session.getTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSessionId(session.getSessionId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUserId(session.getUserId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRefreshTokenHash(session.getRefreshTokenHash());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(session.getStatus());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setIssuedAt(date(session.getIssuedAt()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExpireAt(date(session.getExpireAt()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRefreshExpireAt(date(session.getRefreshExpireAt()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setRevokedAt(date(session.getRevokedAt()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setLastAccessAt(date(session.getLastAccessAt()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(date(session.getCreateTime()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(date(session.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return row;
    }

    private AuthSession toDomain(AuthSessionDO row) {
        // 返回已经完成封装的业务结果。
        return AuthSession.builder()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .id(row.getId())
                // 计算当前分支的中间结果，供后续判断或组装使用。
                .tenantId(row.getTenantId() == null ? 0L : row.getTenantId())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .sessionId(row.getSessionId())
                // 计算当前分支的中间结果，供后续判断或组装使用。
                .userId(row.getUserId() == null ? 0L : row.getUserId())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .refreshTokenHash(row.getRefreshTokenHash())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .status(row.getStatus())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .issuedAt(localDateTime(row.getIssuedAt()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .expireAt(localDateTime(row.getExpireAt()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .refreshExpireAt(localDateTime(row.getRefreshExpireAt()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .revokedAt(localDateTime(row.getRevokedAt()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .lastAccessAt(localDateTime(row.getLastAccessAt()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .createTime(localDateTime(row.getCreateTime()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .updateTime(localDateTime(row.getUpdateTime()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
