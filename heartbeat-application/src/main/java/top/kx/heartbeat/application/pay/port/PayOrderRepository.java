package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.request.PayOrderRequest;

import java.util.List;

/**
 * 定义支付业务持久化端口，隔离应用层与具体数据访问实现。
 */
public interface PayOrderRepository {


    DomainRecord createOrder(PayOrderRequest request);


    DomainRecord getOrder(String id);


    List<DomainRecord> listOrders();


    DomainRecord updateOrderStatus(String orderNo, String status);
}
