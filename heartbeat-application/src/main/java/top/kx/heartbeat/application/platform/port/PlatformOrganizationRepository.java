package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

public interface PlatformOrganizationRepository {

    List<DomainRecord> listDepartments();

    DomainRecord createDepartment(Map<String, Object> command);

    DomainRecord updateDepartment(String id, Map<String, Object> command);

    void deleteDepartment(String id);

    List<DomainRecord> listTenants();

    List<DomainRecord> listPosts();
}
