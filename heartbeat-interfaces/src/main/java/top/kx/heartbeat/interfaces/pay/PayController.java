// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/pay")
public class PayController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PayService payService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/channels")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listChannels() {
        // 注释：返回当前处理结果。
        return listResponse(payService.listChannels());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/channels/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getChannel(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.getChannel(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/channels")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "支付", action = "创建支付渠道")
    public Result<DynamicRecordResponse> createChannel(@RequestBody PayChannelRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.createChannel(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/channels/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "支付", action = "更新支付渠道")
    public Result<DynamicRecordResponse> updateChannel(@PathVariable String id,
                                                       // 注释：声明当前元素使用的注解配置。
                                                       @RequestBody PayChannelRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.updateChannel(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/orders")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:order')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "支付", action = "创建支付订单")
    public Result<DynamicRecordResponse> createOrder(@RequestBody PayOrderRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.createOrder(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/orders")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listOrders() {
        // 注释：返回当前处理结果。
        return listResponse(payService.listOrders());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/orders/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getOrder(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.getOrder(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/orders/{orderNo}/notify")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "支付", action = "处理支付回调")
    public Result<DynamicRecordResponse> notify(@PathVariable String orderNo,
                                                // 注释：声明当前元素使用的注解配置。
                                                @RequestBody PayNotifyRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.handleNotify(orderNo, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/orders/{orderNo}/mock-notify")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "支付", action = "兼容模拟支付回调")
    public Result<DynamicRecordResponse> mockNotify(@PathVariable String orderNo,
                                                    // 注释：声明当前元素使用的注解配置。
                                                    @RequestBody PayNotifyRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(payService.handleNotify(orderNo, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/notify-logs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> notifyLogs() {
        // 注释：返回当前处理结果。
        return listResponse(payService.listNotifyLogs(""));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/orders/{orderNo}/notify-logs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> orderNotifyLogs(@PathVariable String orderNo) {
        // 注释：返回当前处理结果。
        return listResponse(payService.listNotifyLogs(orderNo));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.fromRecordList(records));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.from(record));
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
