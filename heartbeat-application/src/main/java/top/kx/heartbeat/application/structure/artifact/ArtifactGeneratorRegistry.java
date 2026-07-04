package top.kx.heartbeat.application.structure.artifact;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ArtifactGeneratorRegistry {

    @Resource
    private List<ArtifactGenerator> generatorList;
    private Map<String, ArtifactGenerator> generators;

    @PostConstruct
    public void initializeGenerators() {
        Map<String, ArtifactGenerator> registered = new LinkedHashMap<>();
        for (ArtifactGenerator generator : generatorList) {
            ArtifactGenerator previous = registered.put(generator.artifactType(), generator);
            if (previous != null) {
                throw new IllegalStateException("产物生成器类型重复: " + generator.artifactType());
            }
        }
        this.generators = Collections.unmodifiableMap(registered);
    }

    public ArtifactGenerator get(String type) {
        ArtifactGenerator generator = generators.get(type);
        if (generator == null) {
            throw new IllegalArgumentException("不支持的产物类型: " + type);
        }
        return generator;
    }

    public Map<String, ArtifactGenerator> all() {
        return generators;
    }
}
