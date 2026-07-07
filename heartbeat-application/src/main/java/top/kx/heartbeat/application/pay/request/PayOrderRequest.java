package top.kx.heartbeat.application.pay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 承载支付业务请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
