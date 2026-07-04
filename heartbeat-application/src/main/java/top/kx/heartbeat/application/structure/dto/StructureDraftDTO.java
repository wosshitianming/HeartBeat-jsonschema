package top.kx.heartbeat.application.structure.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 结构草稿数据传输对象。
 *
 * <p>用于返回尚未发布为正式版本的结构草稿内容。</p>
 */
@Value
public class StructureDraftDTO {
    /**
     * 草稿结构模型。
     */
    StructureNode structureModel;
    /**
     * 草稿生成配置。
     */
    JsonNode generationConfig;
    /**
     * 草稿字段覆盖配置。
     */
    JsonNode fieldOverrides;
    /**
     * 草稿生成产物。
     */
    Map<String, JsonNode> artifacts;
    /**
     * 草稿生成警告列表。
     */
    List<InferenceWarning> warnings;
    /**
     * 草稿校验模式。
     */
    ValidationMode validationMode;
    /**
     * 草稿样本摘要。
     */
    String sampleDigest;
    /**
     * 草稿更新时间。
     */
    Instant updateTime;
}
