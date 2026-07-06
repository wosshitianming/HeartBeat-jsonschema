package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformRoleRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysRoleDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysRoleDOMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class PlatformRoleRepositoryImpl extends AbstractPlatformRepositorySupport implements PlatformRoleRepository {

    @Resource
    private SysRoleDOMapper roleMapper;

    @Override
    public List<DomainRecord> listRoles() {
        SysRoleDOExample example = new SysRoleDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(roleMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createRole(Map<String, Object> command) {
        return create(roleMapper, new SysRoleDO(), command);
    }

    @Override
    public DomainRecord updateRole(String id, Map<String, Object> command) {
        return update(roleMapper, new SysRoleDO(), id, command);
    }

    @Override
    public void deleteRole(String id) {
        delete(roleMapper, id);
    }
}
