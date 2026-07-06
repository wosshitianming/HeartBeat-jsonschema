package top.kx.heartbeat.application.flow;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.domain.flow.model.ConnectionCredential;
import top.kx.heartbeat.domain.flow.model.ConnectionCredentialStatus;
import top.kx.heartbeat.domain.flow.repository.ConnectionCredentialRepository;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接凭证应用服务。
 *
 * <p>负责连接凭证的查询、保存、删除和连通性校验编排。</p>
 */
@Service
public class ConnectionCredentialService {

    /**
     * 连接凭证仓储。
     */
    @Resource
    private ConnectionCredentialRepository repository;

    /**
     * 查询连接凭证列表。
     *
     * @return 连接凭证列表
     */
    public List<ConnectionCredential> list() {
        // 查询全部连接凭证。
        return repository.findAll();
    }

    /**
     * 保存连接凭证。
     *
     * @param credential 连接凭证
     * @return 保存后的连接凭证
     */
    @Transactional
    public ConnectionCredential save(ConnectionCredential credential) {
        // 获取当前时间。
        Instant now = Instant.now();
        // 判断是否需要初始化连接凭证标识。
        if (StringUtils.isBlank(credential.getId())) {
            // 写入连接凭证创建时间。
            credential.setCreateTime(now);
        }
        // 写入连接凭证更新时间。
        credential.setUpdateTime(now);
        // 判断连接凭证状态是否为空。
        if (StringUtils.isBlank(credential.getStatus())) {
            // 默认设置连接凭证为启用状态。
            credential.setStatus(ConnectionCredentialStatus.ACTIVE.getCode());
        }
        // 判断公开配置是否为空。
        if (credential.getConfig() == null) {
            // 兜底初始化公开配置。
            credential.setConfig(new LinkedHashMap<>());
        }
        // 判断敏感配置是否为空。
        if (credential.getSecrets() == null) {
            // 兜底初始化敏感配置。
            credential.setSecrets(new LinkedHashMap<>());
        }
        // 保存连接凭证。
        return repository.save(credential);
    }

    /**
     * 删除连接凭证。
     *
     * @param id 连接凭证标识
     */
    @Transactional
    public void delete(String id) {
        // 删除连接凭证。
        repository.delete(id);
    }

    /**
     * 测试连接凭证。
     *
     * @param id 连接凭证标识
     * @return 连接凭证测试结果
     */
    public RecordResponse test(String id) {
        // 查询连接凭证。
        ConnectionCredential credential = repository.findById(id)
                // 连接凭证不存在时抛出业务异常。
                .orElseThrow(() -> new IllegalArgumentException("连接凭据不存在: " + id));
        // 创建连接测试结果。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入连接凭证标识。
        result.put("id", credential.getId());
        // 写入连接凭证类型。
        result.put("type", credential.getType());
        // 根据公开配置是否存在判断基础校验是否成功。
        result.put("success", MapUtils.isNotEmpty(credential.getConfig()));
        // 写入当前 MVP 阶段提示消息。
        result.put("message", "MVP 已完成凭据配置校验，真实连接探测将在执行器阶段接入");
        // 返回连接测试结果。
        return RecordResponse.from(result);
    }
}
