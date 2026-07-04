package top.kx.heartbeat.application.structure.dto;

import lombok.Getter;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;

import java.util.Collections;
import java.util.List;

public class InferenceResult {

    @Getter
    private final StructureNode root;
    private final List<InferenceWarning> warnings;

    public InferenceResult(StructureNode root, List<InferenceWarning> warnings) {
        this.root = root;
        this.warnings = warnings;
    }

    public List<InferenceWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
