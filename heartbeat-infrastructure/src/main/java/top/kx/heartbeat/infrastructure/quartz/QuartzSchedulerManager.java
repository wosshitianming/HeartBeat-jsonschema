package top.kx.heartbeat.infrastructure.quartz;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "true")
public class QuartzSchedulerManager implements QuartzJobScheduler {

    @Resource
    private Scheduler scheduler;

    @Override
    public void refreshJobs(List<ScheduledJob> jobs) {
        for (ScheduledJob job : jobs) {
            try {
                scheduleJob(job);
            } catch (SchedulerException ex) {
                log.warn("同步任务失败: {} - {}", job.getJobCode(), ex.getMessage());
            }
        }
    }

    @Override
    public void runNow(String jobId, String jobGroup) {
        try {
            scheduler.triggerJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("手动执行任务失败: " + jobId, ex);
        }
    }

    @Override
    public void pause(String jobId, String jobGroup) {
        try {
            scheduler.pauseJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("暂停任务失败: " + jobId, ex);
        }
    }

    @Override
    public void resume(String jobId, String jobGroup) {
        try {
            scheduler.resumeJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("恢复任务失败: " + jobId, ex);
        }
    }

    private void scheduleJob(ScheduledJob job) throws SchedulerException {
        String jobCode = job.getJobCode();
        String jobGroup = hasText(job.getJobGroup()) ? job.getJobGroup() : "DEFAULT";
        if (!hasText(job.getCronExpression()) || !hasText(job.getInvokeTarget())) {
            return;
        }
        if (!"ACTIVE".equalsIgnoreCase(job.getStatus())
                && !"ENABLED".equalsIgnoreCase(job.getStatus())) {
            deleteJob(jobCode, jobGroup);
            return;
        }
        JobKey jobKey = JobKey.jobKey(jobCode, jobGroup);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobCode, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        JobDetail jobDetail = JobBuilder.newJob(HeartbeatQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData("invokeTarget", job.getInvokeTarget())
                .usingJobData("jobId", jobCode)
                .storeDurably()
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void deleteJob(String jobId, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    private boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }
}
