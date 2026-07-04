package top.kx.heartbeat.application.mp.port;

import java.util.List;
import java.util.Map;

public interface MpMenuSyncGateway {

    Map<String, Object> syncMenus(Map<String, Object> account, List<Map<String, Object>> menus);
}
