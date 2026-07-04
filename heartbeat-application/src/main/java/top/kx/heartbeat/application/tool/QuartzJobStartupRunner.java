package top.kx.heartbeat.application.tool;


import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 应用启动后加载 sys_job 到 Quartz。
 */
@Component
@Conditional(QuartzEnabledCondition.class)
public class QuartzJobStartupRunner implements ApplicationRunner {

    @Resource
    private QuartzJobService quartzJobService;

    @Override
    public void run(ApplicationArguments args) {
        quartzJobService.refreshScheduler();
    }
}
