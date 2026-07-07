package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpAutoReplyRequest;

import java.util.List;

public interface MpAutoReplyRepository {

    List<DomainRecord> listAutoReplies(String accountId);

    DomainRecord saveAutoReply(MpAutoReplyRequest request);
}
