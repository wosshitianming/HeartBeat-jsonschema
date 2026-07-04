package top.kx.heartbeat.infrastructure.quartz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 演示定时任务 Bean：invokeTarget 填写 demoQuartzJob.run。
 */
@Slf4j
@Component("demoQuartzJob")
public class DemoQuartzJob {

    /**
     * 无参演示任务。
     */
    public void run() {
        log.info("DemoQuartzJob 执行成功");
    }
}
