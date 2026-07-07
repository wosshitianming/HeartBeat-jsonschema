package top.kx.heartbeat.infrastructure.flow.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbNodeComponentDO;

import javax.annotation.Resource;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NodeComponentConvert {

    @Resource
    private ObjectMapper objectMapper;

    public abstract NodeComponentManifest toDomain(HbNodeComponentDO entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "manifestJson", source = ".")
    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract HbNodeComponentDO toEntity(NodeComponentManifest manifest);

    @ObjectFactory
    protected NodeComponentManifest createManifest(HbNodeComponentDO entity) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readValue(entity.getManifestJson(), NodeComponentManifest.class);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("组件 Manifest 解析失败", ex);
        }
    }

    protected String writeManifest(NodeComponentManifest manifest) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(manifest);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("组件 Manifest 序列化失败", ex);
        }
    }
}
