package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;

import java.util.List;

public interface PayChannelRepository {

    List<DomainRecord> listChannels();

    DomainRecord getChannel(String id);

    DomainRecord createChannel(PayChannelRequest request);

    DomainRecord updateChannel(String id, PayChannelRequest request);
}
