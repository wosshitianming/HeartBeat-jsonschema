package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

public interface PlatformConfigRepository {

    List<DomainRecord> listConfigurations();

    DomainRecord createConfiguration(Map<String, Object> command);

    DomainRecord updateConfiguration(String id, Map<String, Object> command);

    void deleteConfiguration(String id);

    List<DomainRecord> listDictTypes();

    List<DomainRecord> listDictData();
}
