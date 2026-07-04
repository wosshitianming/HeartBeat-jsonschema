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
    public abstract HbNodeComponentDO toEntity(NodeComponentManifest manifest);

    @ObjectFactory
    protected NodeComponentManifest createManifest(HbNodeComponentDO entity) {
        try {
            return objectMapper.readValue(entity.getManifestJson(), NodeComponentManifest.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("组件 Manifest 解析失败", ex);
        }
    }

    protected String writeManifest(NodeComponentManifest manifest) {
        try {
            return objectMapper.writeValueAsString(manifest);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("组件 Manifest 序列化失败", ex);
        }
    }
}
