package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.request.PayNotifyLogRequest;

import java.util.List;

public interface PayNotifyLogRepository {

    DomainRecord recordNotify(PayNotifyLogRequest request);

    List<DomainRecord> listNotifyLogs(String orderNo);
}
