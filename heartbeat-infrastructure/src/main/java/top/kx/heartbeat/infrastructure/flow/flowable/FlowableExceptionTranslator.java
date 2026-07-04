package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.springframework.stereotype.Component;

/**
 * Flowable 异常翻译器。
 *
 * <p>用于把 Flowable 原始异常翻译为前端可读的 Flow 节点级错误。</p>
 */
@Component
public class FlowableExceptionTranslator {

    /**
     * 翻译 Flowable 异常编码。
     *
     * @param ex Flowable 异常
     * @return HeartBeat 错误编码
     */
    public String translateCode(Throwable ex) {
        // 读取异常消息。
        String message = ex == null ? "" : StringUtils.defaultString(ex.getMessage());
        // 判断是否为无路由命中异常。
        if (StringUtils.containsIgnoreCase(message, "No outgoing sequence flow")) {
            // 返回无路由命中错误编码。
            return "FLOW_NO_ROUTE_MATCHED";
        }
        // 判断是否为消息等待找不到异常。
        if (StringUtils.containsIgnoreCase(message, "Cannot find execution")) {
            // 返回等待实例不存在错误编码。
            return "FLOW_WAIT_NOT_FOUND";
        }
        // 判断是否为 Flowable 原始异常。
        if (ex instanceof FlowableException) {
            // 返回 Flowable 引擎异常编码。
            return "FLOWABLE_ENGINE_ERROR";
        }
        // 返回通用运行时错误编码。
        return "FLOW_RUNTIME_ERROR";
    }

    /**
     * 翻译 Flowable 异常消息。
     *
     * @param ex Flowable 异常
     * @return 用户可读错误消息
     */
    public String translateMessage(Throwable ex) {
        // 翻译错误编码。
        String code = translateCode(ex);
        // 判断无路由命中错误。
        if ("FLOW_NO_ROUTE_MATCHED".equals(code)) {
            // 返回无路由命中中文消息。
            return "流程条件分支没有命中任何出口";
        }
        // 判断等待实例不存在错误。
        if ("FLOW_WAIT_NOT_FOUND".equals(code)) {
            // 返回等待实例不存在中文消息。
            return "流程等待实例不存在或已经恢复";
        }
        // 返回通用中文消息。
        return "流程运行时执行失败";
    }
}
