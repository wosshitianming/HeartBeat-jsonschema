package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformConfigurationRequest;

import java.util.List;

public interface PlatformConfigRepository {

    List<DomainRecord> listConfigurations();

    DomainRecord createConfiguration(PlatformConfigurationRequest request);

    DomainRecord updateConfiguration(String id, PlatformConfigurationRequest request);

    void deleteConfiguration(String id);

    List<DomainRecord> listDictTypes();

    List<DomainRecord> listDictData();
}
