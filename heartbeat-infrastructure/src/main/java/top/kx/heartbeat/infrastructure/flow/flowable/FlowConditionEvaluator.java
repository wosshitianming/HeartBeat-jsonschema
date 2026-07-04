package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Flow 条件表达式受控求值器。
 *
 * <p>用于让 BPMN sequenceFlow 只调用受控 Java Bean，不直接执行用户表达式。</p>
 */
@Service("flowConditionEvaluator")
public class FlowConditionEvaluator {

    /**
     * Flowable 变量编解码器。
     */
    @Resource
    private FlowableVariableCodec variableCodec;

    /**
     * Flow 表达式沙盒。
     */
    @Resource
    private FlowExpressionSandbox expressionSandbox;

    /**
     * 判断指定连线或端口是否命中。
     *
     * @param execution Flowable 执行上下文
     * @param edgeId 连线标识
     * @param sourcePort 源端口
     * @return 是否命中
     */
    public boolean matches(DelegateExecution execution, String edgeId, String sourcePort) {
        // 读取命中端口列表。
        List<String> nextPorts = variableCodec.readNextPorts(execution);
        // 判断连线或源端口是否命中。
        return nextPorts.contains(edgeId) || nextPorts.contains(sourcePort);
    }

    /**
     * 按编码表达式判断条件分支是否命中。
     *
     * @param execution Flowable 执行上下文
     * @param edgeId 连线标识
     * @param sourcePort 源端口
     * @param encodedExpression Base64 编码表达式
     * @return 是否命中
     */
    public boolean matchesEncoded(DelegateExecution execution, String edgeId, String sourcePort, String encodedExpression) {
        // 读取命中端口列表。
        List<String> nextPorts = variableCodec.readNextPorts(execution);
        // 优先使用节点执行器写出的命中端口。
        if (!nextPorts.isEmpty()) {
            // 返回命中端口判断结果。
            return nextPorts.contains(edgeId) || nextPorts.contains(sourcePort);
        }
        // 解码条件表达式。
        String expression = new String(Base64.getDecoder().decode(encodedExpression), StandardCharsets.UTF_8);
        // 执行条件表达式。
        boolean matched = expressionSandbox.evaluate(expression, variableCodec.readPayload(execution));
        // 判断是否为 false 分支。
        if ("false".equalsIgnoreCase(sourcePort)) {
            // false 分支返回取反结果。
            return !matched;
        }
        // true 或默认分支返回表达式结果。
        return matched;
    }

    /**
     * 判断指定连线是否命中。
     *
     * @param execution Flowable 执行上下文
     * @param edgeId 连线标识
     * @return 是否命中
     */
    public boolean matches(DelegateExecution execution, String edgeId) {
        // 复用三参数判断逻辑。
        return matches(execution, edgeId, edgeId);
    }
}
