package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlatformAdminRepository {

    Optional<DomainRecord> findUserByUsername(String username);

    Optional<DomainRecord> findUserById(String userId);

    Optional<DomainRecord> findUserPreference(String userId, String preferenceKey);

    DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue);

    List<String> listPermissionsByUserId(String userId);

    List<String> listRoleCodesByUserId(String userId);

    List<String> listDataScopesByUserId(String userId);

    List<String> listCustomDeptIdsByUserId(String userId);

    List<DomainRecord> listMenus();

    List<DomainRecord> listAuthorizedMenus(String userId);

    DomainRecord createMenu(Map<String, Object> command);

    DomainRecord updateMenu(String id, Map<String, Object> command);

    void deleteMenu(String id);

    boolean roleExists(String roleId);

    List<String> listRoleMenuIds(String roleId);

    void saveRoleMenus(String roleId, List<String> menuIds);

    List<DomainRecord> listUsers();

    DomainRecord createUser(Map<String, Object> command);

    DomainRecord updateUser(String id, Map<String, Object> command);

    void deleteUser(String id);

    List<DomainRecord> listDepartments();

    DomainRecord createDepartment(Map<String, Object> command);

    DomainRecord updateDepartment(String id, Map<String, Object> command);

    void deleteDepartment(String id);

    List<DomainRecord> listRoles();

    DomainRecord createRole(Map<String, Object> command);

    DomainRecord updateRole(String id, Map<String, Object> command);

    void deleteRole(String id);

    List<DomainRecord> listConfigurations();

    DomainRecord createConfiguration(Map<String, Object> command);

    DomainRecord updateConfiguration(String id, Map<String, Object> command);

    void deleteConfiguration(String id);

    List<DomainRecord> listSocialProviders();

    DomainRecord createSocialProvider(Map<String, Object> command);

    DomainRecord updateSocialProvider(String id, Map<String, Object> command);

    void deleteSocialProvider(String id);

    List<DomainRecord> listLoginLogs();

    List<DomainRecord> listTenants();

    List<DomainRecord> listPosts();

    List<DomainRecord> listDictTypes();

    List<DomainRecord> listDictData();

    List<DomainRecord> listNotices();

    List<DomainRecord> listOperationLogs();

    List<DomainRecord> listOnlineSessions();

    List<DomainRecord> listOauthClients();

    void recordLogin(String username, String status, String message);

    List<DomainRecord> listActiveSocialProviders();

    Optional<DomainRecord> findSocialProvider(String provider);

    Optional<DomainRecord> findSocialBind(String provider, String openId);

    DomainRecord saveSocialBind(Map<String, Object> command);

    DomainRecord createSocialUser(Map<String, Object> command);
}
