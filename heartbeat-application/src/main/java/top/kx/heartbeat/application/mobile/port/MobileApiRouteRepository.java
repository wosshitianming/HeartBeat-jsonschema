package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.request.MobileApiRouteRequest;

import java.util.List;

/**
 * 定义移动端配置持久化端口，隔离应用层与具体数据访问实现。
 */
public interface MobileApiRouteRepository {


    List<DomainRecord> listApiRoutes(String appId);


    DomainRecord saveApiRoute(MobileApiRouteRequest request);
}
