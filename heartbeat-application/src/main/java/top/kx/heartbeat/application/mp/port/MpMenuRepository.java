package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;

import java.util.List;

public interface MpMenuRepository {

    List<DomainRecord> listMenus(String accountId);

    DomainRecord saveMenu(MpMenuRequest request);
}
