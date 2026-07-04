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
        Instant startedAt = Instant.now();
        String message = "执行成功";
        String status = "SUCCESS";
        try {
            invoke(invokeTarget);
        } catch (RuntimeException ex) {
            status = "FAIL";
            message = ex.getMessage() == null ? "执行失败" : ex.getMessage();
            throw ex;
        } finally {
            writeLog(jobCode, invokeTarget, status, message, startedAt);
        }
    }

    private void invoke(String invokeTarget) {
        int splitIndex = invokeTarget.lastIndexOf('.');
        if (splitIndex <= 0) {
            throw new IllegalArgumentException("invokeTarget 格式错误: " + invokeTarget);
        }
        String beanName = invokeTarget.substring(0, splitIndex);
        String methodName = invokeTarget.substring(splitIndex + 1);
        Object bean = applicationContext.getBean(beanName);
        Method method = findNoArgMethod(bean.getClass(), methodName);
        try {
            method.invoke(bean);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("调用任务方法失败: " + invokeTarget, ex);
        }
    }

    private Method findNoArgMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到无参方法: " + type.getName() + "#" + methodName);
    }

    private void writeLog(String jobCode, String invokeTarget, String status, String message, Instant startedAt) {
        try {
            ScheduledJob job = scheduledJobRepository.findByCode(jobCode).orElse(null);
            Instant finishedAt = Instant.now();
            scheduledJobRepository.appendExecutionLog(JobExecutionLog.builder()
                    .tenantId(job == null ? null : job.getTenantId())
                    .jobId(job == null ? null : job.getId())
                    .jobCode(jobCode)
                    .invokeTarget(invokeTarget)
                    .resultStatus(status)
                    .message(message)
                    .durationMs(finishedAt.toEpochMilli() - startedAt.toEpochMilli())
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("写入任务日志失败: {}", ex.getMessage());
        }
    }
}
