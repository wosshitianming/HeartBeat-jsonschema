package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.platform.port.PlatformLoginLogRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysLoginLogDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysLoginLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;


/**
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformLoginLogRepositoryImpl implements PlatformLoginLogRepository {

    @Resource
    private SysLoginLogDOMapper loginLogMapper;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param username 登录用户名。
     * @param status 目标业务状态。
     * @param message 业务处理所需参数。
     */
    @Override
    public void recordLogin(String username, String status, String message) {
        SysLoginLogDO row = new SysLoginLogDO();
        row.setTenantId(tenantId());
        row.setUsername(username);
        //TODO 参数不明
//        row.setLoginName(username);
        row.setResultStatus(status);
//        row.setLoginStatus(status);
//        row.setMessage(message);
//        row.setLoginMessage(message);
        row.setCreateTime(new Date());
        row.setUpdateTime(new Date());
        loginLogMapper.insertSelective(row);
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long current = TenantContext.getTenantId();
        return current == null ? 1L : current;
    }
}
