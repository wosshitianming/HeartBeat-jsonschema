package top.kx.heartbeat.application.structure.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构预览数据传输对象。
 *
 * <p>用于返回样本推断后的结构模型、产物、警告和样本摘要。</p>
 */
@Getter
public class StructurePreviewDTO {

    /**
     * 推断生成的结构模型。
     */
    private final StructureNode structureModel;

    /**
     * 推断过程生成的结构产物。
     */
    private final Map<String, JsonNode> artifacts;

    /**
     * 推断过程产生的警告列表。
     */
    private final List<InferenceWarning> warnings;

    /**
     * 样本数据摘要。
     */
    private final String sampleDigest;

    /**
     * 创建结构预览数据传输对象。
     *
     * @param structureModel 推断生成的结构模型
     * @param artifacts 推断过程生成的结构产物
     * @param warnings 推断过程产生的警告列表
     * @param sampleDigest 样本数据摘要
     */
    public StructurePreviewDTO(StructureNode structureModel,
                               Map<String, JsonNode> artifacts,
                               List<InferenceWarning> warnings,
                               String sampleDigest) {
        // 写入推断生成的结构模型。
        this.structureModel = structureModel;
        // 复制并锁定结构产物，避免调用方修改内部状态。
        this.artifacts = Collections.unmodifiableMap(new LinkedHashMap<>(artifacts));
        // 锁定警告列表，避免调用方修改内部状态。
        this.warnings = Collections.unmodifiableList(warnings);
        // 写入样本数据摘要。
        this.sampleDigest = sampleDigest;
    }
}
