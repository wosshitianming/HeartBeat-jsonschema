package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flow 等待订阅登记监听器。
 *
 * <p>用于在流程进入等待节点时生成执行级 waitInstanceId，为 Inbox 早到事件匹配做准备。</p>
 */
@Component("flowWaitRegistrationListener")
public class FlowWaitRegistrationListener implements ExecutionListener {

    /**
     * 等待实例变量名。
     */
    private static final String WAIT_INSTANCE_ID = "hbWaitInstanceId";

    /**
     * 登记等待订阅。
     *
     * @param execution Flowable 执行上下文
     */
    @Override
    public void notify(DelegateExecution execution) {
        // 生成等待实例标识。
        String waitInstanceId = UUID.randomUUID().toString();
        // 写入等待实例变量。
        execution.setVariableLocal(WAIT_INSTANCE_ID, waitInstanceId);
        // 写入等待订阅摘要。
        execution.setVariableLocal("hbWaitSubscription", createSubscription(execution, waitInstanceId));
    }

    /**
     * 创建等待订阅摘要。
     *
     * @param execution Flowable 执行上下文
     * @param waitInstanceId 等待实例标识
     * @return 等待订阅摘要
     */
    private Map<String, Object> createSubscription(DelegateExecution execution, String waitInstanceId) {
        // 创建订阅摘要。
        Map<String, Object> subscription = new LinkedHashMap<>();
        // 写入流程实例标识。
        subscription.put("processInstanceId", execution.getProcessInstanceId());
        // 写入执行标识。
        subscription.put("executionId", execution.getId());
        // 写入活动标识。
        subscription.put("activityId", execution.getCurrentActivityId());
        // 写入等待实例标识。
        subscription.put("waitInstanceId", waitInstanceId);
        // 返回订阅摘要。
        return subscription;
    }
}
