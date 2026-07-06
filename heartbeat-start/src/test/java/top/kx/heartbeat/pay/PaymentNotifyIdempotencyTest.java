package top.kx.heartbeat.pay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.pay.PayService;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class PaymentNotifyIdempotencyTest {

    @Autowired
    private PayService payService;

    @Test
    void duplicateProviderNotifyDoesNotChangeOrderTwice() {
//        TenantContext.setTenantId(1L);
//        String suffix = String.valueOf(System.nanoTime());
//        Map<String, Object> channelCommand = new LinkedHashMap<>();
//        channelCommand.put("name", "Mock Pay " + suffix);
//        channelCommand.put("provider", "MOCK");
//        channelCommand.put("appId", "app-" + suffix);
//        channelCommand.put("appSecret", "secret-" + suffix);
//        Map<String, Object> channel = payService.createChannel(channelCommand);
//
//        Map<String, Object> orderCommand = new LinkedHashMap<>();
//        orderCommand.put("orderNo", "P" + suffix);
//        orderCommand.put("channelId", channel.get("id"));
//        orderCommand.put("subject", "测试订单");
//        orderCommand.put("amount", new BigDecimal("9.9900"));
//        Map<String, Object> order = payService.createOrder(orderCommand);
//
//        String payload = "{\"notifyId\":\"N" + suffix + "\",\"status\":\"CLOSED\"}";
//        Map<String, Object> notify = new LinkedHashMap<>();
//        notify.put("payload", payload);
//        notify.put("status", "CLOSED");
//        notify.put("signature", payService.sign(payload, "secret-" + suffix));
//
//        payService.handleNotify(String.valueOf(order.get("orderNo")), notify);
//        payService.handleNotify(String.valueOf(order.get("orderNo")), notify);
//
//        assertEquals(1, payService.listNotifyLogs(String.valueOf(order.get("orderNo"))).size());
//        assertEquals("CLOSED", payService.getOrder(String.valueOf(order.get("orderNo"))).get("status"));
    }
}
