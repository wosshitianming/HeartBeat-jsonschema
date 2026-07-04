package top.kx.heartbeat.tool;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.kx.heartbeat.infrastructure.persistence.mapper.tool.SysJobLogMapper;
import top.kx.heartbeat.infrastructure.persistence.query.QueryWrapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BackendToolsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobLogMapper sysJobLogMapper;

    @Test
    void importsDatabaseMetadataForFlexCodeGeneration() throws Exception {
        mockMvc.perform(post("/api/v1/tool/gen/tables/import")
                        .param("tableName", "SYS_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SYS_USER"));

        mockMvc.perform(get("/api/v1/tool/gen/imported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].payload").isNotEmpty());
    }

    @Test
    void refreshesControlsAndRunsQuartzJobsWithPersistentLogs() throws Exception {
        mockMvc.perform(post("/api/v1/tool/jobs/refresh"))
                .andExpect(status().isOk());

        TriggerKey triggerKey = TriggerKey.triggerKey("job-demo", "DEFAULT");
        assertTrue(scheduler.checkExists(triggerKey));

        mockMvc.perform(post("/api/v1/tool/jobs/job-demo/pause"))
                .andExpect(status().isOk());
        assertEquals(Trigger.TriggerState.PAUSED, scheduler.getTriggerState(triggerKey));

        mockMvc.perform(post("/api/v1/tool/jobs/job-demo/resume"))
                .andExpect(status().isOk());
        assertEquals(Trigger.TriggerState.NORMAL, scheduler.getTriggerState(triggerKey));

        mockMvc.perform(post("/api/v1/tool/jobs/job-demo/run"))
                .andExpect(status().isOk());

        boolean logged = false;
        for (int i = 0; i < 30; i++) {
            long count = TenantContext.runAsPlatform(() ->
                    sysJobLogMapper.selectCountByQuery(
                            QueryWrapper.create().where("invoke_target", "demoQuartzJob.run")));
            if (count > 0) {
                logged = true;
                break;
            }
            Thread.sleep(100);
        }
        assertTrue(logged, "Quartz execution should persist a job log");
    }
}
