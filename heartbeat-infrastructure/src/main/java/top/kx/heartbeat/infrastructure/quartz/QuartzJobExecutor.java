package top.kx.heartbeat.infrastructure.quartz;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.tool.ScheduledJobRepository;
import top.kx.heartbeat.domain.tool.model.JobExecutionLog;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.Instant;

@Slf4j
@Component
public class QuartzJobExecutor {

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private ScheduledJobRepository scheduledJobRepository;

    public void execute(String invokeTarget, String jobCode) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Instant startedAt = Instant.now();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String message = "执行成功";
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String status = "SUCCESS";
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            invoke(invokeTarget);
        } catch (RuntimeException ex) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            status = "FAIL";
            // 计算当前分支的中间结果，供后续判断或组装使用。
            message = ex.getMessage() == null ? "执行失败" : ex.getMessage();
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw ex;
        } finally {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            writeLog(jobCode, invokeTarget, status, message, startedAt);
        }
    }

    private void invoke(String invokeTarget) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        int splitIndex = invokeTarget.lastIndexOf('.');
        // 根据当前业务条件选择对应处理路径。
        if (splitIndex <= 0) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("invokeTarget 格式错误: " + invokeTarget);
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String beanName = invokeTarget.substring(0, splitIndex);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String methodName = invokeTarget.substring(splitIndex + 1);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Object bean = applicationContext.getBean(beanName);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Method method = findNoArgMethod(bean.getClass(), methodName);
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            method.invoke(bean);
        } catch (ReflectiveOperationException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("调用任务方法失败: " + invokeTarget, ex);
        }
    }

    private Method findNoArgMethod(Class<?> type, String methodName) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Method method : type.getMethods()) {
            // 比对当前业务状态，决定是否进入该处理分支。
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                // 返回已经完成封装的业务结果。
                return method;
            }
        }
        // 对非法业务状态立即失败，避免错误继续扩散。
        throw new IllegalArgumentException("未找到无参方法: " + type.getName() + "#" + methodName);
    }

    private void writeLog(String jobCode, String invokeTarget, String status, String message, Instant startedAt) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
            ScheduledJob job = scheduledJobRepository.findByCode(jobCode).orElse(null);
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Instant finishedAt = Instant.now();
            // 追加代码或文本片段，逐步生成最终内容。
            scheduledJobRepository.appendExecutionLog(JobExecutionLog.builder()
                    // 计算当前分支的中间结果，供后续判断或组装使用。
                    .tenantId(job == null ? null : job.getTenantId())
                    // 计算当前分支的中间结果，供后续判断或组装使用。
                    .jobId(job == null ? null : job.getId())
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .jobCode(jobCode)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .invokeTarget(invokeTarget)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .resultStatus(status)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .message(message)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .durationMs(finishedAt.toEpochMilli() - startedAt.toEpochMilli())
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .startedAt(startedAt)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .finishedAt(finishedAt)
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    .build());
        } catch (RuntimeException ex) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            log.warn("写入任务日志失败: {}", ex.getMessage());
        }
    }
}
