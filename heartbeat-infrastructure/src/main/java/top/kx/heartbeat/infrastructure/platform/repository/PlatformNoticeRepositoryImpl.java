// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformNoticeRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysNoticeDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysNoticeDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysNoticeDOMapper;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformNoticeRepositoryImpl implements PlatformNoticeRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysNoticeDOMapper noticeMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listNotices() {
        // 注释：返回当前处理结果。
        return noticeMapper.selectByExampleWithBLOBs(new SysNoticeDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::record)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysNoticeDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("noticeTitle", row.getNoticeTitle());
        // 注释：执行当前代码行。
        values.put("noticeType", row.getNoticeType());
        // 注释：执行当前代码行。
        values.put("publishScope", row.getPublishScope());
        // 注释：执行当前代码行。
        values.put("publishStatus", row.getPublishStatus());
        // 注释：执行当前代码行。
        values.put("publishedAt", row.getPublishedAt());
        // 注释：执行当前代码行。
        values.put("expiredAt", row.getExpiredAt());
        // 注释：执行当前代码行。
        values.put("noticeContent", row.getNoticeContent());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
