package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformNoticeRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysNoticeDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysNoticeDOMapper;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class PlatformNoticeRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformNoticeRepository {

    @Resource
    private SysNoticeDOMapper noticeMapper;

    @Override
    public List<DomainRecord> listNotices() {
        return records(noticeMapper.selectByExample(new SysNoticeDOExample()));
    }
}
