package top.kx.heartbeat.application.pay.request;

import lombok.Data;

@Data
public class PayNotifyRequest {

    private String payload;
    private String signature;
    private String status;
}
