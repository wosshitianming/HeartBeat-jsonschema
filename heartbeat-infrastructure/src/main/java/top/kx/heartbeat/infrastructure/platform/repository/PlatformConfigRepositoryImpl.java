package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformConfigRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDictItemDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDictTypeDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysConfigDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDictItemDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDictTypeDOMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class PlatformConfigRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformConfigRepository {

    @Resource
    private SysConfigDOMapper configMapper;
    @Resource
    private SysDictTypeDOMapper dictTypeMapper;
    @Resource
    private SysDictItemDOMapper dictItemMapper;

    @Override
    public List<DomainRecord> listConfigurations() {
        return records(configMapper.selectByExample(new SysConfigDOExample()));
    }

    @Override
    public DomainRecord createConfiguration(Map<String, Object> command) {
        return create(configMapper, new SysConfigDO(), command);
    }

    @Override
    public DomainRecord updateConfiguration(String id, Map<String, Object> command) {
        return update(configMapper, new SysConfigDO(), id, command);
    }

    @Override
    public void deleteConfiguration(String id) {
        delete(configMapper, id);
    }

    @Override
    public List<DomainRecord> listDictTypes() {
        return records(dictTypeMapper.selectByExample(new SysDictTypeDOExample()));
    }

    @Override
    public List<DomainRecord> listDictData() {
        return records(dictItemMapper.selectByExample(new SysDictItemDOExample()));
    }
}
