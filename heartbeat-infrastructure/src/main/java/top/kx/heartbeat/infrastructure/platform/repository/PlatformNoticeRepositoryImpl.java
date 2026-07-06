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

@Repository
public class PlatformNoticeRepositoryImpl implements PlatformNoticeRepository {

    @Resource
    private SysNoticeDOMapper noticeMapper;

    @Override
    public List<DomainRecord> listNotices() {
        return noticeMapper.selectByExampleWithBLOBs(new SysNoticeDOExample())
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    private DomainRecord record(SysNoticeDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("noticeTitle", row.getNoticeTitle());
        values.put("noticeType", row.getNoticeType());
        values.put("publishScope", row.getPublishScope());
        values.put("publishStatus", row.getPublishStatus());
        values.put("publishedAt", row.getPublishedAt());
        values.put("expiredAt", row.getExpiredAt());
        values.put("noticeContent", row.getNoticeContent());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }
}
