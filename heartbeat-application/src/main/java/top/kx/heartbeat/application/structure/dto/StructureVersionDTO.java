package top.kx.heartbeat.application.structure.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 结构版本数据传输对象。
 *
 * <p>用于返回单个结构版本的模型、配置、产物和样本摘要。</p>
 */
@Value
public class StructureVersionDTO {
    /**
     * 结构版本号。
     */
    int versionNo;
    /**
     * 结构版本模型。
     */
    StructureNode structureModel;
    /**
     * 结构生成配置。
     */
    JsonNode generationConfig;
    /**
     * 字段配置覆盖项。
     */
    JsonNode fieldOverrides;
    /**
     * 结构生成产物。
     */
    Map<String, JsonNode> artifacts;
    /**
     * 结构生成警告列表。
     */
    List<InferenceWarning> warnings;
    /**
     * 结构校验模式。
     */
    ValidationMode validationMode;
    /**
     * 样本数据摘要。
     */
    String sampleDigest;
    /**
     * 结构版本创建时间。
     */
    Instant createTime;
}
