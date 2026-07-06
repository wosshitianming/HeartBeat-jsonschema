package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;

public interface PlatformAuditQueryRepository {

    List<DomainRecord> listLoginLogs();

    List<DomainRecord> listOperationLogs();

    List<DomainRecord> listOnlineSessions();

    List<DomainRecord> listOauthClients();
}
