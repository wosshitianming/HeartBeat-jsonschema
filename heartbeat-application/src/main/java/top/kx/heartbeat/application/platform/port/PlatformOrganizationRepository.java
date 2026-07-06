package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformDepartmentRequest;

import java.util.List;

public interface PlatformOrganizationRepository {

    List<DomainRecord> listDepartments();

    DomainRecord createDepartment(PlatformDepartmentRequest request);

    DomainRecord updateDepartment(String id, PlatformDepartmentRequest request);

    void deleteDepartment(String id);

    List<DomainRecord> listTenants();

    List<DomainRecord> listPosts();
}
