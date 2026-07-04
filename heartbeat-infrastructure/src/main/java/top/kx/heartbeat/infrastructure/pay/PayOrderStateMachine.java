package top.kx.heartbeat.infrastructure.pay;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.pay.PayOrderStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderEntity;

import java.time.LocalDateTime;

/**
 * 支付订单状态机。
 *
 * <p>负责统一校验和执行支付订单状态流转。</p>
 */
@Component
public class PayOrderStateMachine {

    /**
     * 执行支付订单状态流转。
     *
     * @param order 支付订单实体
     * @param targetStatus 目标支付状态
     */
    public void transit(PayOrderEntity order, PayOrderStatus targetStatus) {
        // 解析当前支付订单状态。
        PayOrderStatus currentStatus = PayOrderStatus.fromCode(order.getStatus());
        // 当前状态与目标状态一致时直接结束。
        if (currentStatus == targetStatus) {
            // 保持状态幂等，不重复更新时间。
            return;
        }
        // 校验当前状态是否允许流转到目标状态。
        if (!currentStatus.canTransitTo(targetStatus)) {
            // 非法流转直接抛出业务异常。
            throw new IllegalStateException("非法支付状态流转: " + currentStatus.getCode() + " -> " + targetStatus.getCode());
        }
        // 获取本次状态变更时间。
        LocalDateTime now = LocalDateTime.now();
        // 写入目标状态编码。
        order.setStatus(targetStatus.getCode());
        // 写入订单更新时间。
        order.setUpdateTime(now);
        // 判断目标状态是否为已支付。
        if (targetStatus == PayOrderStatus.PAID) {
            // 写入支付完成时间。
            order.setPaidAt(now);
        }
    }
}
