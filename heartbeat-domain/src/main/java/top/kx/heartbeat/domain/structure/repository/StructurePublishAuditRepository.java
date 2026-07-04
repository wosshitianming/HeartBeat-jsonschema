package top.kx.heartbeat.domain.structure.repository;

/**
 * 结构化定义发布审计领域仓储接口
 * <p>
 * application 层通过该接口落地发布/激活操作的审计记录，由 infrastructure 层实现。
 * </p>
 *
 * @author heartbeat-team
 */
public interface StructurePublishAuditRepository {

    /**
     * 记录一次发布/激活审计
     *
     * @param definitionId 结构化定义 ID
     * @param versionNo    涉及版本号
     * @param operatorId   操作人用户 ID
     * @param status       状态（SUCCESS/FAILED）
     * @param summary      摘要（变更条目或失败原因）
     */
    void record(long definitionId, int versionNo, long operatorId, String status, String summary);
}
