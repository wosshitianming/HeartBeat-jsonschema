package top.kx.heartbeat.interfaces.pay;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.pay.PayService;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 支付管理接口控制器。
 *
 * <p>负责支付渠道、支付订单和支付通知日志的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/pay")
public class PayController {

    /**
     * 支付应用服务。
     */
    @Resource
    private PayService payService;

    /**
     * 查询支付渠道列表。
     *
     * @return 支付渠道列表响应
     */
    @GetMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listChannels() {
        // 查询支付渠道动态记录列表。
        List<RecordResponse> channels = payService.listChannels();
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(channels);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询支付渠道详情。
     *
     * @param id 支付渠道标识
     * @return 支付渠道详情响应
     */
    @GetMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getChannel(@PathVariable String id) {
        // 查询支付渠道动态记录。
        RecordResponse channel = payService.getChannel(id);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(channel);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 创建支付渠道。
     *
     * @param request 支付渠道创建参数
     * @return 支付渠道创建结果响应
     */
    @PostMapping("/channels")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "创建支付渠道")
    public Result<DynamicRecordResponse> createChannel(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 创建支付渠道动态记录。
        RecordResponse channel = payService.createChannel(payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(channel);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 更新支付渠道。
     *
     * @param id 支付渠道标识
     * @param request 支付渠道更新参数
     * @return 支付渠道更新结果响应
     */
    @PutMapping("/channels/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:edit')")
    @OperLog(module = "支付", action = "更新支付渠道")
    public Result<DynamicRecordResponse> updateChannel(@PathVariable String id, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 更新支付渠道动态记录。
        RecordResponse channel = payService.updateChannel(id, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(channel);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 创建支付订单。
     *
     * @param request 支付订单创建参数
     * @return 支付订单创建结果响应
     */
    @PostMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:order')")
    @OperLog(module = "支付", action = "创建支付订单")
    public Result<DynamicRecordResponse> createOrder(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 创建支付订单动态记录。
        RecordResponse order = payService.createOrder(payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(order);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询支付订单列表。
     *
     * @return 支付订单列表响应
     */
    @GetMapping("/orders")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> listOrders() {
        // 查询支付订单动态记录列表。
        List<RecordResponse> orders = payService.listOrders();
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(orders);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询支付订单详情。
     *
     * @param id 支付订单标识
     * @return 支付订单详情响应
     */
    @GetMapping("/orders/{id}")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<DynamicRecordResponse> getOrder(@PathVariable String id) {
        // 查询支付订单动态记录。
        RecordResponse order = payService.getOrder(id);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(order);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 处理支付通知。
     *
     * @param orderNo 支付订单号
     * @param request 支付通知参数
     * @return 支付通知处理结果响应
     */
    @PostMapping("/orders/{orderNo}/notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "处理支付回调")
    public Result<DynamicRecordResponse> notify(@PathVariable String orderNo, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 处理支付通知并返回订单动态记录。
        RecordResponse order = payService.handleNotify(orderNo, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(order);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 处理模拟支付通知。
     *
     * @param orderNo 支付订单号
     * @param request 模拟通知参数
     * @return 模拟通知处理结果响应
     */
    @PostMapping("/orders/{orderNo}/mock-notify")
    @PreAuthorize("@permissionGuard.has('biz:pay:notify')")
    @OperLog(module = "支付", action = "兼容模拟支付回调")
    public Result<DynamicRecordResponse> mockNotify(@PathVariable String orderNo, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 处理模拟支付通知并返回订单动态记录。
        RecordResponse order = payService.handleNotify(orderNo, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(order);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询全部支付通知日志。
     *
     * @return 支付通知日志列表响应
     */
    @GetMapping("/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> notifyLogs() {
        // 查询全部支付通知日志动态记录。
        List<RecordResponse> logs = payService.listNotifyLogs("");
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(logs);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询指定订单支付通知日志。
     *
     * @param orderNo 支付订单号
     * @return 支付通知日志列表响应
     */
    @GetMapping("/orders/{orderNo}/notify-logs")
    @PreAuthorize("@permissionGuard.has('biz:pay:list')")
    public Result<List<DynamicRecordResponse>> orderNotifyLogs(@PathVariable String orderNo) {
        // 查询指定订单支付通知日志动态记录。
        List<RecordResponse> logs = payService.listNotifyLogs(orderNo);
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(logs);
        // 返回统一接口响应。
        return Result.success(response);
    }
}
