package top.kx.heartbeat.application.flow.runtime;

import org.apache.commons.lang3.StringUtils;
import top.kx.heartbeat.domain.flow.model.*;

import java.util.*;

/**
 * 代码节点组件清单构建器。
 *
 * <p>将组件默认值、端口和配置 JSON Schema 收敛到简短的 Java DSL。</p>
 */
public final class NodeManifestBuilder {

    private static final String DEFAULT_VERSION = "1.0.0";

    private final NodeComponentManifest manifest = new NodeComponentManifest();
    private final List<ComponentPort> inputs = new ArrayList<>();
    private final List<ComponentPort> outputs = new ArrayList<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final Set<String> requiredProperties = new LinkedHashSet<>();
    private final List<String> capabilities = new ArrayList<>();

    private NodeManifestBuilder(NodeExecutor executor, String type, String name) {
        if (executor == null) {
            throw new IllegalArgumentException("节点执行器不能为空");
        }
        manifest.setType(required(type, "节点类型"));
        manifest.setName(required(name, "节点名称"));
        manifest.setVersion(DEFAULT_VERSION);
        manifest.setCategory("自定义");
        manifest.setDescription("");
        manifest.setIcon("code");
        manifest.setSource(NodeComponentSource.CODE.getCode());
        manifest.setStatus(NodeComponentStatus.ACTIVE.getCode());
        ComponentRuntime runtime = new ComponentRuntime();
        runtime.setExecutor(required(executor.executorId(), "执行器标识"));
        runtime.setMode(Arrays.asList("debug", "production"));
        manifest.setRuntime(runtime);
    }

    /**
     * 创建与执行器绑定的代码节点清单。
     */
    public static NodeManifestBuilder codeNode(NodeExecutor executor, String type, String name) {
        return new NodeManifestBuilder(executor, type, name);
    }

    private static String required(String value, String label) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    public NodeManifestBuilder version(String version) {
        manifest.setVersion(required(version, "节点版本"));
        return this;
    }

    public NodeManifestBuilder category(String category) {
        manifest.setCategory(required(category, "节点分类"));
        return this;
    }

    public NodeManifestBuilder description(String description) {
        manifest.setDescription(StringUtils.defaultString(description));
        return this;
    }

    public NodeManifestBuilder icon(String icon) {
        manifest.setIcon(required(icon, "节点图标"));
        return this;
    }

    public NodeManifestBuilder sortNo(int sortNo) {
        manifest.setSortNo(sortNo);
        return this;
    }

    public NodeManifestBuilder input(String id, String label, String schema) {
        inputs.add(port(id, label, schema, false));
        return this;
    }

    public NodeManifestBuilder requiredInput(String id, String label, String schema) {
        inputs.add(port(id, label, schema, true));
        return this;
    }

    public NodeManifestBuilder output(String id, String label, String schema) {
        outputs.add(port(id, label, schema, false));
        return this;
    }

    public NodeManifestBuilder stringProperty(String name, String title, boolean required) {
        return stringProperty(name, title, null, required);
    }

    public NodeManifestBuilder stringProperty(String name, String title, String defaultValue, boolean required) {
        String propertyName = required(name, "配置字段名称");
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("title", required(title, "配置字段标题"));
        if (defaultValue != null) {
            property.put("default", defaultValue);
        }
        properties.put(propertyName, property);
        if (required) {
            requiredProperties.add(propertyName);
        }
        return this;
    }

    public NodeManifestBuilder capability(String capability) {
        String value = required(capability, "节点能力");
        if (!capabilities.contains(value)) {
            capabilities.add(value);
        }
        return this;
    }

    public NodeComponentManifest build() {
        validatePorts(inputs, "输入");
        validatePorts(outputs, "输出");
        manifest.setId("code:" + manifest.getType() + ":" + manifest.getVersion());
        manifest.setPorts(new ComponentPorts(new ArrayList<>(inputs), new ArrayList<>(outputs)));
        manifest.getRuntime().setCapabilities(new ArrayList<>(capabilities));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>(properties));
        if (!requiredProperties.isEmpty()) {
            schema.put("required", new ArrayList<>(requiredProperties));
        }
        manifest.setConfigSchema(schema);
        return manifest;
    }

    private ComponentPort port(String id, String label, String schema, boolean required) {
        return new ComponentPort(
                required(id, "端口标识"),
                required(label, "端口名称"),
                StringUtils.defaultIfBlank(schema, "object"),
                required);
    }

    private void validatePorts(List<ComponentPort> ports, String direction) {
        Set<String> ids = new LinkedHashSet<>();
        for (ComponentPort port : ports) {
            if (!ids.add(port.getId())) {
                throw new IllegalStateException(direction + "端口标识重复: " + port.getId());
            }
        }
    }
}
