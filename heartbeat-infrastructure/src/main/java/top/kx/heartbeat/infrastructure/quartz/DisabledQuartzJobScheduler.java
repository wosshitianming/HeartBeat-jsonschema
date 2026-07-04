package top.kx.heartbeat.infrastructure.quartz;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledQuartzJobScheduler implements QuartzJobScheduler {

    private static final String MESSAGE = "Quartz 未启用";

    @Override
    public void refreshJobs(List<ScheduledJob> jobs) {
        throw disabled();
    }

    @Override
    public void runNow(String jobId, String jobGroup) {
        throw disabled();
    }

    @Override
    public void pause(String jobId, String jobGroup) {
        throw disabled();
    }

    @Override
    public void resume(String jobId, String jobGroup) {
        throw disabled();
    }

    private IllegalStateException disabled() {
        return new IllegalStateException(MESSAGE);
    }
}
