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
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysLoginLogDO row = new SysLoginLogDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUsername(username);
        //TODO 参数不明
//        row.setLoginName(username);
        row.setResultStatus(status);
//        row.setLoginStatus(status);
//        row.setMessage(message);
//        row.setLoginMessage(message);
        row.setCreateTime(new Date());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(new Date());
        // 将当前业务变更写入持久化层，保持数据状态同步。
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
