package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformRoleRequest;

import java.util.List;

public interface PlatformRoleRepository {

    List<DomainRecord> listRoles();

    DomainRecord createRole(PlatformRoleRequest request);

    DomainRecord updateRole(String id, PlatformRoleRequest request);

    void deleteRole(String id);
}
