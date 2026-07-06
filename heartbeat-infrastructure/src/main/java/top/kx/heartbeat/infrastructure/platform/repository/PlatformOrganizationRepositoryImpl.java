package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformOrganizationRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDeptDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDeptDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysPostDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysTenantDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDeptDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysPostDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysTenantDOMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class PlatformOrganizationRepositoryImpl extends AbstractPlatformRepositorySupport
        implements PlatformOrganizationRepository {

    @Resource
    private SysDeptDOMapper deptMapper;
    @Resource
    private SysTenantDOMapper tenantMapper;
    @Resource
    private SysPostDOMapper postMapper;

    @Override
    public List<DomainRecord> listDepartments() {
        SysDeptDOExample example = new SysDeptDOExample();
        example.setOrderByClause("sort_no ASC, id ASC");
        return records(deptMapper.selectByExample(example));
    }

    @Override
    public DomainRecord createDepartment(Map<String, Object> command) {
        return create(deptMapper, new SysDeptDO(), command);
    }

    @Override
    public DomainRecord updateDepartment(String id, Map<String, Object> command) {
        return update(deptMapper, new SysDeptDO(), id, command);
    }

    @Override
    public void deleteDepartment(String id) {
        delete(deptMapper, id);
    }

    @Override
    public List<DomainRecord> listTenants() {
        return records(tenantMapper.selectByExample(new SysTenantDOExample()));
    }

    @Override
    public List<DomainRecord> listPosts() {
        return records(postMapper.selectByExample(new SysPostDOExample()));
    }
}
