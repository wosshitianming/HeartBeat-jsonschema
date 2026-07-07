package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;

import java.util.List;

/**
 * 定义公众号管理持久化端口，隔离应用层与具体数据访问实现。
 */
public interface MpMenuRepository {


    List<DomainRecord> listMenus(String accountId);


    DomainRecord saveMenu(MpMenuRequest request);
}
