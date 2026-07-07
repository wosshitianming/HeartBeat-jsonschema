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

/**
 * 提供支付业务 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/pay")
public class PayController {

    @Resource
    private PayService payService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托支付业务应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listChannels() {
        return listResponse(payService.listChannels());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，并统一委托支付业务应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getChannel(@PathVariable String id) {
        return recordResponse(payService.getChannel(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托支付业务应用服务完成处理。
     *
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "创建支付渠道")
    public Result<DynamicRecordResponse> createChannel(@RequestBody PayChannelRequest request) {
        return recordResponse(payService.createChannel(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托支付业务应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "更新支付渠道")
    public Result<DynamicRecordResponse> updateChannel(@PathVariable String id,
                                                       @RequestBody PayChannelRequest request) {
        return recordResponse(payService.updateChannel(id, request));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托支付业务应用服务完成处理。
     *
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:order')")
    @OperLog(module = "支付", action = "创建支付订单")
    public Result<DynamicRecordResponse> createOrder(@RequestBody PayOrderRequest request) {
        return recordResponse(payService.createOrder(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托支付业务应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listOrders() {
        return listResponse(payService.listOrders());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，并统一委托支付业务应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/orders/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getOrder(@PathVariable String id) {
        return recordResponse(payService.getOrder(id));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托支付业务应用服务完成处理。
     *
     * @param orderNo 业务处理所需参数。
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/orders/{orderNo}/notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "处理支付回调")
    public Result<DynamicRecordResponse> notify(@PathVariable String orderNo,
                                                @RequestBody PayNotifyRequest request) {
        return recordResponse(payService.handleNotify(orderNo, request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托支付业务应用服务完成处理。
     *
     * @param orderNo 业务处理所需参数。
     * @param request 支付业务请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/orders/{orderNo}/mock-notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "兼容模拟支付回调")
    public Result<DynamicRecordResponse> mockNotify(@PathVariable String orderNo,
                                                    @RequestBody PayNotifyRequest request) {
        return recordResponse(payService.handleNotify(orderNo, request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托支付业务应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> notifyLogs() {
        return listResponse(payService.listNotifyLogs(""));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托支付业务应用服务完成处理。
     *
     * @param orderNo 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @GetMapping("/orders/{orderNo}/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> orderNotifyLogs(@PathVariable String orderNo) {
        return listResponse(payService.listNotifyLogs(orderNo));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托支付业务应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托支付业务应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
