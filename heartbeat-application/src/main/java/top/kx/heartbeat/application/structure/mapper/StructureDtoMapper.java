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

    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract StructureDraftDTO toDTO(StructureDraft draft);

    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract StructureVersionDTO toDTO(StructureVersion version);

    protected JsonNode mapJson(String value) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value == null) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }

    protected Map<String, JsonNode> mapArtifacts(Map<String, String> artifacts) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (artifacts == null) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, JsonNode> result = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put(artifact.getKey(), mapJson(artifact.getValue()));
        }
        // 返回已经完成封装的业务结果。
        return result;
    }
}
