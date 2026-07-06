package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformMenuRequest;

import java.util.List;

public interface PlatformMenuRepository {

    List<DomainRecord> listMenus();

    List<DomainRecord> listAuthorizedMenus(String userId);

    DomainRecord createMenu(PlatformMenuRequest request);

    DomainRecord updateMenu(String id, PlatformMenuRequest request);

    void deleteMenu(String id);
}
