package top.kx.heartbeat.domain.flow.repository;

import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowVersion;

import java.util.List;
import java.util.Optional;

/**
 * 流程领域仓储接口
 * <p>
 * application 层通过该接口与基础设施层解耦，由 FlowDefinitionRepositoryImpl 在 infrastructure 层实现。
 * </p>
 *
 * @author heartbeat-team
 */
public interface FlowRepository {

    /**
     * 查询全部流程定义
     */
    List<FlowDefinition> findAll();

    /**
     * 按主键查询
     */
    Optional<FlowDefinition> findById(String id);

    /**
     * 按编码查询
     */
    Optional<FlowDefinition> findByCode(String code);

    /**
     * 保存草稿
     */
    FlowDefinition saveDraft(FlowDefinition definition);

    /**
     * 按主键删除
     */
    int deleteById(String id);

    /**
     * 查询某流程的全部版本
     */
    List<FlowVersion> findVersions(String flowId);

    /**
     * 按 (flowId, versionNo) 查询单个版本
     */
    Optional<FlowVersion> findVersion(String flowId, int versionNo);

    /**
     * 保存版本
     */
    FlowVersion saveVersion(FlowVersion version);

    /**
     * 更新流程版本运行时部署信息
     */
    void updateVersionRuntime(FlowVersion version);

    /**
     * 激活版本
     */
    void activateVersion(String flowId, int versionNo);

    void deactivate(String flowId);

    /**
     * 更新流程定义当前激活运行时部署信息
     */
    void updateActiveRuntimeDeployment(String flowId,
                                       String runtimeEngine,
                                       String activeDeploymentId,
                                       String activeProcessDefinitionId);

    /**
     * 分页查询（pageNum 从 1 开始）
     */
    Page<FlowDefinition> pageByQuery(String nameLike, String codeEqual, String statusEqual,
                                     String orderByColumn, String orderByDirection,
                                     int pageNum, int pageSize);

    /**
     * 简易分页结果（避免 application 依赖 pagehelper）
     */
    class Page<T> {
        private final List<T> records;
        private final long total;
        private final int pageNum;
        private final int pageSize;

        public Page(List<T> records, long total, int pageNum, int pageSize) {
            this.records = records;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getRecords() {
            return records;
        }

        public long getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
