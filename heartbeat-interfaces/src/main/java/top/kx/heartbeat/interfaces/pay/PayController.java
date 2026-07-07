package top.kx.heartbeat.interfaces.pay;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.pay.PayService;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;
import top.kx.heartbeat.application.pay.request.PayNotifyRequest;
import top.kx.heartbeat.application.pay.request.PayOrderRequest;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pay")
public class PayController {

    @Resource
    private PayService payService;

    @GetMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listChannels() {
        return listResponse(payService.listChannels());
    }

    @GetMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getChannel(@PathVariable String id) {
        return recordResponse(payService.getChannel(id));
    }

    @PostMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "创建支付渠道")
    public Result<DynamicRecordResponse> createChannel(@RequestBody PayChannelRequest request) {
        return recordResponse(payService.createChannel(request));
    }

    @PutMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "更新支付渠道")
    public Result<DynamicRecordResponse> updateChannel(@PathVariable String id,
                                                       @RequestBody PayChannelRequest request) {
        return recordResponse(payService.updateChannel(id, request));
    }

    @PostMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:order')")
    @OperLog(module = "支付", action = "创建支付订单")
    public Result<DynamicRecordResponse> createOrder(@RequestBody PayOrderRequest request) {
        return recordResponse(payService.createOrder(request));
    }

    @GetMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listOrders() {
        return listResponse(payService.listOrders());
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getOrder(@PathVariable String id) {
        return recordResponse(payService.getOrder(id));
    }

    @PostMapping("/orders/{orderNo}/notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "处理支付回调")
    public Result<DynamicRecordResponse> notify(@PathVariable String orderNo,
                                                @RequestBody PayNotifyRequest request) {
        return recordResponse(payService.handleNotify(orderNo, request));
    }

    @PostMapping("/orders/{orderNo}/mock-notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "兼容模拟支付回调")
    public Result<DynamicRecordResponse> mockNotify(@PathVariable String orderNo,
                                                    @RequestBody PayNotifyRequest request) {
        return recordResponse(payService.handleNotify(orderNo, request));
    }

    @GetMapping("/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> notifyLogs() {
        return listResponse(payService.listNotifyLogs(""));
    }

    @GetMapping("/orders/{orderNo}/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> orderNotifyLogs(@PathVariable String orderNo) {
        return listResponse(payService.listNotifyLogs(orderNo));
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
