package top.kx.heartbeat.infrastructure.persistence.entity.pay;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayNotifyLogEntity {
    private Long id;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private String provider;
    private String notifyId;
    private String notifyPayload;
    private String signatureValid;
    private String status;
    private LocalDateTime createTime;
}
