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
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformNoticeRepositoryImpl implements PlatformNoticeRepository {

    @Resource
    private SysNoticeDOMapper noticeMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listNotices() {
        // 返回已经完成封装的业务结果。
        return noticeMapper.selectByExampleWithBLOBs(new SysNoticeDOExample())
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysNoticeDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("noticeTitle", row.getNoticeTitle());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("noticeType", row.getNoticeType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("publishScope", row.getPublishScope());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("publishStatus", row.getPublishStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("publishedAt", row.getPublishedAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("expiredAt", row.getExpiredAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("noticeContent", row.getNoticeContent());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }
}
