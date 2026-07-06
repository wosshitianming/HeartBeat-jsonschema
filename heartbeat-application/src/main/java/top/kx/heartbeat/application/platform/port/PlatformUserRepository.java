package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlatformUserRepository {

    Optional<DomainRecord> findUserByUsername(String username);

    Optional<DomainRecord> findUserById(String userId);

    Optional<DomainRecord> findUserPreference(String userId, String preferenceKey);

    DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue);

    List<DomainRecord> listUsers();

    DomainRecord createUser(Map<String, Object> command);

    DomainRecord updateUser(String id, Map<String, Object> command);

    void deleteUser(String id);

    DomainRecord createSocialUser(Map<String, Object> command);
}
