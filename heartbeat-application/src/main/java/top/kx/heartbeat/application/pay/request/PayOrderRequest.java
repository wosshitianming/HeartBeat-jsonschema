package top.kx.heartbeat.application.pay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayOrderRequest {

    @JsonAlias("order_no")
    private String orderNo;
    @JsonAlias("channel_id")
    private String channelId;
    private String subject;
    private BigDecimal amount;
    private String currency;
    private String status;
    @JsonAlias("client_ip")
    private String clientIp;
    private Object extra;
}
