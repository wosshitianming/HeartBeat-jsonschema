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
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, ArtifactGenerator> registered = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (ArtifactGenerator generator : generatorList) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            ArtifactGenerator previous = registered.put(generator.artifactType(), generator);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (previous != null) {
                // 对非法业务状态立即失败，避免错误继续扩散。
                throw new IllegalStateException("产物生成器类型重复: " + generator.artifactType());
            }
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
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
