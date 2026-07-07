package top.kx.heartbeat.application.pay.request;

import lombok.Data;

@Data
public class PayNotifyLogRequest {

    private String orderId;
    private String orderNo;
    private String provider;
    private String payload;
    private String status;
    private String signatureValid;
}
