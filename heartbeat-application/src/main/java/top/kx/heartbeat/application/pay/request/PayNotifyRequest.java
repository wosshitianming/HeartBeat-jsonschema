package top.kx.heartbeat.application.pay.request;

import lombok.Data;

/**
 * 承载支付业务请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class PayNotifyRequest {

    private String payload;
    private String signature;
    private String status;
}
