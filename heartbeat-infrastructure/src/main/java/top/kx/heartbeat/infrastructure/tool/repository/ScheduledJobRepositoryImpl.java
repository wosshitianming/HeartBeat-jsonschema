package top.kx.heartbeat.infrastructure.tool.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.tool.ScheduledJobRepository;
import top.kx.heartbeat.domain.tool.model.JobExecutionLog;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysJobDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysJobDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysJobLogDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysJobDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysJobLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 定时任务仓储实现。
 * <p>
 * 负责将定时任务领域模型映射到 MBG 生成的 sys_job 和 sys_job_log 表对象。
 * </p>
 */
@Repository
public class ScheduledJobRepositoryImpl implements ScheduledJobRepository {

    /**
     * 默认租户标识。
     */
    private static final long DEFAULT_TENANT_ID = 1L;

    /**
     * 默认操作人标识。
     */
    private static final long DEFAULT_OPERATOR_ID = 1L;

    /**
     * 定时任务 MBG Mapper。
     */
    @Resource
    private SysJobDOMapper jobMapper;

    /**
     * 定时任务日志 MBG Mapper。
     */
    @Resource
    private SysJobLogDOMapper jobLogMapper;

    /**
     * 查询当前租户下的全部定时任务。
     *
     * @return 定时任务列表
     */
    @Override
    public List<ScheduledJob> findAll() {
        // 创建当前租户下的有效任务查询条件。
        SysJobDOExample example = activeJobExample();
        // 按主键倒序返回最新任务。
        example.setOrderByClause("id DESC");
        // 查询任务配置并转换为领域模型列表。
        return jobMapper.selectByExample(example)
                .stream()
                .map(this::toDomainJob)
                .collect(Collectors.toList());
    }

    /**
     * 按任务编码查询定时任务。
     *
     * @param jobCode 任务编码
     * @return 定时任务
     */
    @Override
    public Optional<ScheduledJob> findByCode(String jobCode) {
        // 创建当前租户下的有效任务查询条件。
        SysJobDOExample example = activeJobExample();
        // 追加任务编码匹配条件。
        example.getOredCriteria().get(0).andJobCodeEqualTo(jobCode);
        // 查询任务配置并转换为领域模型。
        return jobMapper.selectByExample(example)
                .stream()
                .findFirst()
                .map(this::toDomainJob);
    }

    /**
     * 追加定时任务执行日志。
     *
     * @param log 执行日志
     */
    @Override
    public void appendExecutionLog(JobExecutionLog log) {
        // 创建定时任务日志持久化对象。
        SysJobLogDO row = new SysJobLogDO();
        // 生成本次日志记录的统一时间。
        Date now = new Date();
        // 写入租户标识。
        row.setTenantId(log.getTenantId() == null ? currentTenantId() : log.getTenantId());
        // 写入任务主键。
        row.setJobId(log.getJobId());
        // 写入任务编码。
        row.setJobCode(log.getJobCode());
        // 写入调用目标。
        row.setInvokeTarget(log.getInvokeTarget());
        // 写入执行结果状态。
        row.setResultStatus(log.getResultStatus());
        // 写入执行消息。
        row.setMessage(log.getMessage());
        // 写入执行耗时。
        row.setDurationMs(log.getDurationMs());
        // 写入开始时间。
        row.setStartedAt(toDate(log.getStartedAt()));
        // 写入完成时间。
        row.setFinishedAt(toDate(log.getFinishedAt()));
        // 写入创建时间。
        row.setCreateTime(now);
        // 写入更新时间。
        row.setUpdateTime(now);
        // 写入默认创建人。
        row.setCreateBy(DEFAULT_OPERATOR_ID);
        // 写入默认更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 持久化定时任务执行日志。
        jobLogMapper.insertSelective(row);
    }

    /**
     * 创建当前租户有效任务查询条件。
     *
     * @return 定时任务查询条件
     */
    private SysJobDOExample activeJobExample() {
        // 创建定时任务查询条件对象。
        SysJobDOExample example = new SysJobDOExample();
        // 限定当前租户下未逻辑删除的任务。
        example.createCriteria()
                .andTenantIdEqualTo(currentTenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回有效任务查询条件。
        return example;
    }

    /**
     * 转换为领域定时任务。
     *
     * @param row MBG 定时任务对象
     * @return 领域定时任务
     */
    private ScheduledJob toDomainJob(SysJobDO row) {
        // 构建领域定时任务对象。
        return ScheduledJob.builder()
                .id(row.getId())
                .tenantId(row.getTenantId())
                .jobCode(row.getJobCode())
                .jobName(row.getJobName())
                .jobGroup(row.getJobGroup())
                .invokeTarget(row.getInvokeTarget())
                .cronExpression(row.getCronExpression())
                .misfirePolicy(row.getMisfirePolicy())
                .concurrent(Boolean.TRUE.equals(row.getConcurrent()))
                .status(row.getStatus())
                .build();
    }

    /**
     * 转换时间。
     *
     * @param value 时间戳
     * @return 日期
     */
    private Date toDate(Instant value) {
        // 将空时间戳保持为空，否则转换为 Date。
        return value == null ? null : Date.from(value);
    }

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识
     */
    private Long currentTenantId() {
        // 读取线程上下文中的租户标识。
        Long tenantId = TenantContext.getTenantId();
        // 上下文为空时回退到系统默认租户。
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
