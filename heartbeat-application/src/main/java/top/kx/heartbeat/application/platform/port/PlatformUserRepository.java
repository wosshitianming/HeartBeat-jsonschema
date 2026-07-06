package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformUserRequest;

import java.util.List;
import java.util.Optional;

public interface PlatformUserRepository {

    Optional<DomainRecord> findUserByUsername(String username);

    Optional<DomainRecord> findUserById(String userId);

    Optional<DomainRecord> findUserPreference(String userId, String preferenceKey);

    DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue);

    List<DomainRecord> listUsers();

    DomainRecord createUser(PlatformUserRequest request);

    DomainRecord updateUser(String id, PlatformUserRequest request);

    void deleteUser(String id);

    DomainRecord createSocialUser(PlatformUserRequest request);
}
