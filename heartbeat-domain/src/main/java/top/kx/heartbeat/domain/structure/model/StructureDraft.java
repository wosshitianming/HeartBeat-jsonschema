package top.kx.heartbeat.domain.structure.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可反复覆盖的结构草稿，不影响当前上线版本。
 */
@Getter
public class StructureDraft {

    private final StructureNode structureModel;
    private final String generationConfig;
    private final String fieldOverrides;
    private final Map<String, String> artifacts;
    private final List<InferenceWarning> warnings;
    private final String validationMode;
    private final String sampleDigest;
    private final Instant updateTime;

    public StructureDraft(StructureNode structureModel,
                          String generationConfig,
                          String fieldOverrides,
                          Map<String, String> artifacts,
                          List<InferenceWarning> warnings,
                          String validationMode,
                          String sampleDigest,
                          Instant updateTime) {
        this.structureModel = structureModel;
        this.generationConfig = generationConfig;
        this.fieldOverrides = fieldOverrides;
        this.artifacts = Collections.unmodifiableMap(new LinkedHashMap<>(artifacts));
        this.warnings = Collections.unmodifiableList(warnings);
        this.validationMode = validationMode;
        this.sampleDigest = sampleDigest;
        this.updateTime = updateTime;
    }

}
