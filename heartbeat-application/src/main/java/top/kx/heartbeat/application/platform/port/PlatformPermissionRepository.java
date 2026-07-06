package top.kx.heartbeat.application.platform.port;

import java.util.List;

public interface PlatformPermissionRepository {

    List<String> listPermissionsByUserId(String userId);

    List<String> listRoleCodesByUserId(String userId);

    List<String> listDataScopesByUserId(String userId);

    List<String> listCustomDeptIdsByUserId(String userId);

    boolean roleExists(String roleId);

    List<String> listRoleMenuIds(String roleId);

    void saveRoleMenus(String roleId, List<String> menuIds);
}
