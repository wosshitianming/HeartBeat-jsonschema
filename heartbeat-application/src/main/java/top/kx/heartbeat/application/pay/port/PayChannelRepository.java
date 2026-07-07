package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;

import java.util.List;

/**
 * 定义支付业务持久化端口，隔离应用层与具体数据访问实现。
 */
public interface PayChannelRepository {


    List<DomainRecord> listChannels();


    DomainRecord getChannel(String id);


    DomainRecord createChannel(PayChannelRequest request);


    DomainRecord updateChannel(String id, PayChannelRequest request);
}
