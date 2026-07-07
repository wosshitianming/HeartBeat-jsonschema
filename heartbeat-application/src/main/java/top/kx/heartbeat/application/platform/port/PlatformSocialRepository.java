package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformSocialBindRequest;
import top.kx.heartbeat.application.platform.request.PlatformSocialProviderRequest;

import java.util.List;
import java.util.Optional;

/**
 * 定义平台管理持久化端口，隔离应用层与具体数据访问实现。
 */
public interface PlatformSocialRepository {


    List<DomainRecord> listSocialProviders();


    DomainRecord createSocialProvider(PlatformSocialProviderRequest request);


    DomainRecord updateSocialProvider(String id, PlatformSocialProviderRequest request);


    void deleteSocialProvider(String id);


    List<DomainRecord> listActiveSocialProviders();


    Optional<DomainRecord> findSocialProvider(String provider);


    Optional<DomainRecord> findSocialBind(String provider, String openId);


    DomainRecord saveSocialBind(PlatformSocialBindRequest request);
}
