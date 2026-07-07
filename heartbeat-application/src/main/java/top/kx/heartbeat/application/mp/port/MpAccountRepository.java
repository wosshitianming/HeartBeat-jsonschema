package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpAccountRequest;

import java.util.List;

public interface MpAccountRepository {

    List<DomainRecord> listAccounts();

    DomainRecord getAccount(String id);

    DomainRecord saveAccount(MpAccountRequest request);
}
