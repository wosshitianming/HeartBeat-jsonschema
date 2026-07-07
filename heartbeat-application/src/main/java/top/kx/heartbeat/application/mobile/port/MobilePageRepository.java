package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.request.MobilePageRequest;

import java.util.List;

public interface MobilePageRepository {

    List<DomainRecord> listPages(String appId);

    DomainRecord savePage(MobilePageRequest request);
}
