package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.platform.port.PlatformLoginLogRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysLoginLogDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysLoginLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 平台登录日志仓储实现。
 */
@Repository
public class PlatformLoginLogRepositoryImpl implements PlatformLoginLogRepository {

    @Resource
    private SysLoginLogDOMapper loginLogMapper;

    @Override
    public void recordLogin(String username, String status, String message) {
        SysLoginLogDO row = new SysLoginLogDO();
        row.setTenantId(tenantId());
        row.setUsername(username);
        row.setLoginName(username);
        row.setResultStatus(status);
        row.setLoginStatus(status);
        row.setMessage(message);
        row.setLoginMessage(message);
        row.setCreateTime(new Date());
        row.setUpdateTime(new Date());
        loginLogMapper.insertSelective(row);
    }

    private Long tenantId() {
        Long current = TenantContext.getTenantId();
        return current == null ? 1L : current;
    }
}
