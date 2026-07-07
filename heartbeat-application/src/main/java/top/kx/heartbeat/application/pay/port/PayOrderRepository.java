package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.request.PayOrderRequest;

import java.util.List;

public interface PayOrderRepository {

    DomainRecord createOrder(PayOrderRequest request);

    DomainRecord getOrder(String id);

    List<DomainRecord> listOrders();

    DomainRecord updateOrderStatus(String orderNo, String status);
}
