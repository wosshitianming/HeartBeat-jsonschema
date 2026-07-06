package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

public interface PlatformMenuRepository {

    List<DomainRecord> listMenus();

    List<DomainRecord> listAuthorizedMenus(String userId);

    DomainRecord createMenu(Map<String, Object> command);

    DomainRecord updateMenu(String id, Map<String, Object> command);

    void deleteMenu(String id);
}
