package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.response.RecordResponse;

import java.util.List;
import java.util.Map;

public interface MpMenuSyncGateway {

    RecordResponse syncMenus(Map<String, Object> account, List<Map<String, Object>> menus);
}
