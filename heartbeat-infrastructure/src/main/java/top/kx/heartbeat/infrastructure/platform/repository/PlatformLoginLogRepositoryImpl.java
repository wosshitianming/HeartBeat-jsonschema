// 注释：声明当前文件所属的包路径。
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

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformLoginLogRepositoryImpl implements PlatformLoginLogRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysLoginLogDOMapper loginLogMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void recordLogin(String username, String status, String message) {
        // 注释：设置或计算当前变量值。
        SysLoginLogDO row = new SysLoginLogDO();
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setUsername(username);
        //TODO 参数不明
//        row.setLoginName(username);
        // 注释：执行当前代码行。
        row.setResultStatus(status);
//        row.setLoginStatus(status);
//        row.setMessage(message);
//        row.setLoginMessage(message);
        // 注释：执行当前代码行。
        row.setCreateTime(new Date());
        // 注释：执行当前代码行。
        row.setUpdateTime(new Date());
        // 注释：执行当前代码行。
        loginLogMapper.insertSelective(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long current = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return current == null ? 1L : current;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
