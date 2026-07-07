package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.request.MobileApiRouteRequest;

import java.util.List;

public interface MobileApiRouteRepository {

    List<DomainRecord> listApiRoutes(String appId);

    DomainRecord saveApiRoute(MobileApiRouteRequest request);
}
