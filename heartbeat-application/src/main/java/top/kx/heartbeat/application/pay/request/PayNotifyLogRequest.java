package top.kx.heartbeat.application.pay.request;

import lombok.Data;

/**
 * 承载支付业务请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PayNotifyLogRequest {

    private String orderId;
    private String orderNo;
    private String provider;
    private String payload;
    private String status;
    private String signatureValid;
}
