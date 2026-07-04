package top.kx.heartbeat.domain.structure.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 由样例推断出的格式无关结构节点。
 */
@Getter
public class StructureNode {

    private final String path;
    private final String name;
    @Getter(AccessLevel.NONE)
    private final Set<StructureType> types = new LinkedHashSet<>();
    @Getter(AccessLevel.NONE)
    private final Map<String, StructureNode> properties = new LinkedHashMap<>();
    @Setter
    private StructureNode items;
    @Setter
    private boolean required;
    @Setter
    private boolean nullable;
    @Setter
    private int occurrenceCount;
    @Setter
    private int totalSampleCount;

    public StructureNode(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public Set<StructureType> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public Map<String, StructureNode> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void addType(StructureType type) {
        types.add(type);
    }

    public void removeType(StructureType type) {
        types.remove(type);
    }

    public void putProperty(String propertyName, StructureNode node) {
        properties.put(propertyName, node);
    }

}
