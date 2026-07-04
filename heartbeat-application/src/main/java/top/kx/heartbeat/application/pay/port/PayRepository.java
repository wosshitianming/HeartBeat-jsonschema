package top.kx.heartbeat.application.pay.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 支付用用网关接口
 *
 * @author heartbeat-team
 */
public interface PayRepository {

    /**
     * 列出全部支付渠道
     */
    List<DomainRecord> listChannels();

    /**
     * 查询单个支付渠道
     */
    DomainRecord getChannel(String id);

    /**
     * 创建支付渠道
     */
    DomainRecord createChannel(Map<String, Object> command);

    /**
     * 更新支付渠道
     */
    DomainRecord updateChannel(String id, Map<String, Object> command);

    /**
     * 创建支付订单
     */
    DomainRecord createOrder(Map<String, Object> command);

    /**
     * 查询支付订单
     */
    DomainRecord getOrder(String id);

    /**
     * 列出全部支付订单
     */
    List<DomainRecord> listOrders();

    /**
     * 处理支付回调
     *
     * @param orderNo         订单号
     * @param status          渠道返回状态
     * @param payload         原始回调 payload
     * @param signatureValid  签名校验结果
     */
    DomainRecord applyNotify(String orderNo, String status, String payload, String signatureValid);

    /**
     * 列出订单的回调日志
     */
    List<DomainRecord> listNotifyLogs(String orderNo);
}
