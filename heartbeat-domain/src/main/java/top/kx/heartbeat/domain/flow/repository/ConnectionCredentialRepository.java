package top.kx.heartbeat.domain.flow.repository;

import top.kx.heartbeat.domain.flow.model.ConnectionCredential;

import java.util.List;
import java.util.Optional;

/**
 * 外部连接凭据领域仓储接口
 *
 * @author heartbeat-team
 */
public interface ConnectionCredentialRepository {

    /**
     * 列出全部连接凭据
     */
    List<ConnectionCredential> findAll();

    /**
     * 按主键查询
     */
    Optional<ConnectionCredential> findById(String id);

    /**
     * 保存或更新
     */
    ConnectionCredential save(ConnectionCredential credential);

    /**
     * 按主键删除
     */
    void delete(String id);
}
