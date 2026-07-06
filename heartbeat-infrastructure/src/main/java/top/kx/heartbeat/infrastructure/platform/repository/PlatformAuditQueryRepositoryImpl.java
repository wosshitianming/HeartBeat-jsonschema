package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformAuditQueryRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthOauthClientDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSessionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysLoginLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysOperLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthOauthClientDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSessionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysLoginLogDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysOperLogDOMapper;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class PlatformAuditQueryRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformAuditQueryRepository {

    @Resource
    private SysLoginLogDOMapper loginLogMapper;
    @Resource
    private SysOperLogDOMapper operLogMapper;
    @Resource
    private AuthSessionDOMapper sessionMapper;
    @Resource
    private AuthOauthClientDOMapper oauthClientMapper;

    @Override
    public List<DomainRecord> listLoginLogs() {
        SysLoginLogDOExample example = new SysLoginLogDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(loginLogMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listOperationLogs() {
        SysOperLogDOExample example = new SysOperLogDOExample();
        example.setOrderByClause("create_time DESC, id DESC");
        return records(operLogMapper.selectByExample(example));
    }

    @Override
    public List<DomainRecord> listOnlineSessions() {
        return records(sessionMapper.selectByExample(new AuthSessionDOExample()));
    }

    @Override
    public List<DomainRecord> listOauthClients() {
        return records(oauthClientMapper.selectByExample(new AuthOauthClientDOExample()));
    }
}
