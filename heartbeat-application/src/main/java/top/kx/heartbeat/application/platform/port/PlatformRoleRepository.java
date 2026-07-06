package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

public interface PlatformRoleRepository {

    List<DomainRecord> listRoles();

    DomainRecord createRole(Map<String, Object> command);

    DomainRecord updateRole(String id, Map<String, Object> command);

    void deleteRole(String id);
}
