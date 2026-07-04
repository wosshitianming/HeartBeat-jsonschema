package top.kx.heartbeat.application.structure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.application.structure.dto.StructureDefinitionDTO;
import top.kx.heartbeat.application.structure.dto.StructureDraftDTO;
import top.kx.heartbeat.application.structure.dto.StructureVersionDTO;
import top.kx.heartbeat.domain.structure.model.StructureDefinition;
import top.kx.heartbeat.domain.structure.model.StructureDraft;
import top.kx.heartbeat.domain.structure.model.StructureVersion;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class StructureDtoMapper {

    @Resource
    private ObjectMapper objectMapper;

    public abstract StructureDefinitionDTO toDTO(StructureDefinition definition);

    public abstract StructureDraftDTO toDTO(StructureDraft draft);

    public abstract StructureVersionDTO toDTO(StructureVersion version);

    protected JsonNode mapJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }

    protected Map<String, JsonNode> mapArtifacts(Map<String, String> artifacts) {
        if (artifacts == null) {
            return null;
        }
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            result.put(artifact.getKey(), mapJson(artifact.getValue()));
        }
        return result;
    }
}
