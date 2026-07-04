package top.kx.heartbeat.domain.flow.repository;

import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;

import java.util.List;
import java.util.Optional;

/**
 * 节点组件元数据领域仓储接口
 *
 * @author heartbeat-team
 */
public interface NodeComponentRepository {

    /**
     * 列出所有启用的节点组件
     */
    List<NodeComponentManifest> findAllActive();

    /**
     * 按 (type, version) 查询
     */
    Optional<NodeComponentManifest> findByTypeAndVersion(String type, String version);

    /**
     * 保存或更新
     */
    NodeComponentManifest save(NodeComponentManifest manifest);
}
