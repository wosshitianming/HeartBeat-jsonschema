package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;

import java.util.List;

public interface MobileAppRepository {

    List<DomainRecord> listApps();

    DomainRecord saveApp(MobileAppRequest request);
}
