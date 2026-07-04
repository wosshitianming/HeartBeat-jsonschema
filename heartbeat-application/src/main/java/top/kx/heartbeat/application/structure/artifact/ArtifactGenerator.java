package top.kx.heartbeat.application.structure.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import top.kx.heartbeat.application.structure.dto.GenerationOptions;
import top.kx.heartbeat.domain.structure.model.StructureNode;

public interface ArtifactGenerator {

    String artifactType();

    JsonNode generate(StructureNode model, GenerationOptions options);
}
