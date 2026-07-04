package top.kx.heartbeat.application.structure.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

@Getter
public class GenerationOptions {

    private final ValidationMode validationMode;
    private final JsonNode fieldOverrides;

    public GenerationOptions(ValidationMode validationMode, JsonNode fieldOverrides) {
        this.validationMode = validationMode == null ? ValidationMode.LENIENT : validationMode;
        this.fieldOverrides = fieldOverrides == null ? MissingNode.getInstance() : fieldOverrides;
    }

    public static GenerationOptions of(ValidationMode validationMode) {
        return new GenerationOptions(validationMode, MissingNode.getInstance());
    }

    public JsonNode getUiOverrides() {
        return fieldOverrides;
    }

    public ObjectNode toGenerationConfig(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("validationMode", validationMode.name());
        config.put("sampleValuePolicy", "EXAMPLE_ONLY");
        config.put("displayMode", "FORM_PREVIEW");
        return config;
    }
}
