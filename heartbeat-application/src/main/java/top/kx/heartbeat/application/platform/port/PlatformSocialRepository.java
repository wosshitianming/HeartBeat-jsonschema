package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlatformSocialRepository {

    List<DomainRecord> listSocialProviders();

    DomainRecord createSocialProvider(Map<String, Object> command);

    DomainRecord updateSocialProvider(String id, Map<String, Object> command);

    void deleteSocialProvider(String id);

    List<DomainRecord> listActiveSocialProviders();

    Optional<DomainRecord> findSocialProvider(String provider);

    Optional<DomainRecord> findSocialBind(String provider, String openId);

    DomainRecord saveSocialBind(Map<String, Object> command);
}
