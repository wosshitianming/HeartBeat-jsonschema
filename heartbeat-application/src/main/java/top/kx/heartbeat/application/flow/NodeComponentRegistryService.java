package top.kx.heartbeat.application.flow;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.repository.NodeComponentRepository;

import javax.annotation.Resource;
import java.util.*;

/**
 * 节点组件注册应用服务。
 *
 * <p>负责内置组件初始化、组件查询和组件注册。</p>
 */
@Service
public class NodeComponentRegistryService {

    /**
     * 节点组件仓储。
     */
    @Resource
    private NodeComponentRepository repository;

    /**
     * 注册系统内置节点组件。
     */
    @Transactional
    public void registerBuiltIns() {
        // 遍历系统内置组件清单。
        for (NodeComponentManifest manifest : builtInComponents()) {
            // 判断同类型同版本组件是否已经存在。
            if (!repository.findByTypeAndVersion(manifest.getType(), manifest.getVersion()).isPresent()) {
                // 保存缺失的内置组件。
                repository.save(manifest);
            }
        }
    }

    /**
     * 查询启用节点组件列表。
     *
     * @return 启用节点组件列表
     */
    public List<NodeComponentManifest> listActive() {
        // 查询全部启用节点组件。
        return repository.findAllActive();
    }

    /**
     * 注册节点组件。
     *
     * @param manifest 节点组件清单
     * @return 注册后的节点组件清单
     */
    @Transactional
    public NodeComponentManifest register(NodeComponentManifest manifest) {
        // 判断组件版本是否为空。
        if (StringUtils.isBlank(manifest.getVersion())) {
            // 设置默认组件版本。
            manifest.setVersion("1.0.0");
        }
        // 判断组件状态是否为空。
        if (StringUtils.isBlank(manifest.getStatus())) {
            // 设置默认启用状态。
            manifest.setStatus(NodeComponentStatus.ACTIVE.getCode());
        }
        // 判断组件来源是否为空。
        if (StringUtils.isBlank(manifest.getSource())) {
            // 设置默认数据库注册来源。
            manifest.setSource(NodeComponentSource.DATABASE.getCode());
        }
        // 保存节点组件。
        return repository.save(manifest);
    }

    /**
     * 构建系统内置节点组件清单。
     *
     * @return 系统内置节点组件清单
     */
    private List<NodeComponentManifest> builtInComponents() {
        // 创建内置组件清单列表。
        List<NodeComponentManifest> manifests = new ArrayList<>();
        // 添加手动触发组件。
        manifests.add(component("cmp-trigger-manual", "trigger.manual", "触发器", "手动触发", "手动输入 payload 发起调试流程",
                "play", ports(noInputs(), outputs(port("out", "输出", "object"))), runtime("builtin:trigger.manual", "source"), objectSchema()));
        // 添加 Webhook 触发组件。
        manifests.add(component("cmp-trigger-webhook", "trigger.webhook", "触发器", "Webhook", "接收外部 HTTP 回调触发流程",
                "webhook", ports(noInputs(), outputs(port("out", "请求体", "object"))), runtime("builtin:trigger.webhook", "source"), objectSchema()));
        // 添加 MySQL 查询组件。
        manifests.add(component("cmp-source-mysql-query", "source.mysql.query", "数据源", "MySQL 查询", "执行 SQL 并输出结果集",
                "database", ports(inputs(port("in", "输入", "object")), outputs(port("out", "结果", "array"), port("error", "错误", "object"))),
                runtime("builtin:mysql.query", "io", "batch"), schema("connectionId", "sql")));
        // 添加 Redis 读取组件。
        manifests.add(component("cmp-source-redis-get", "source.redis.get", "数据源", "Redis 读取", "读取 Redis key",
                "redis", ports(inputs(port("in", "输入", "object")), outputs(port("out", "值", "object"), port("error", "错误", "object"))),
                runtime("builtin:redis.get", "io"), schema("connectionId", "key")));
        // 添加 Redis 写入组件。
        manifests.add(component("cmp-sink-redis-set", "sink.redis.set", "输出", "Redis 写入", "写入 Redis key/value",
                "redis", ports(inputs(port("in", "输入", "object")), outputs(port("out", "输出", "object"), port("error", "错误", "object"))),
                runtime("builtin:redis.set", "io"), schema("connectionId", "key", "value")));
        // 添加 MQ 消费组件。
        manifests.add(component("cmp-source-mq-consume", "source.mq.consume", "消息", "MQ 消费", "消费消息并触发流程",
                "mq", ports(noInputs(), outputs(port("out", "消息", "object"), port("error", "错误", "object"))),
                runtime("builtin:mq.consume", "source", "stream"), schema("connectionId", "topic")));
        // 添加 MQ 投递组件。
        manifests.add(component("cmp-sink-mq-publish", "sink.mq.publish", "输出", "MQ 投递", "投递消息到 MQ topic",
                "mq", ports(inputs(port("in", "输入", "object")), outputs(port("out", "输出", "object"), port("error", "错误", "object"))),
                runtime("builtin:mq.publish", "io"), schema("connectionId", "topic")));
        // 添加 HTTP 请求组件。
        manifests.add(component("cmp-action-http-request", "action.http.request", "动作", "HTTP 请求", "调用外部或内部 HTTP API",
                "http", ports(inputs(port("in", "输入", "object")), outputs(port("out", "响应", "object"), port("error", "错误", "object"))),
                runtime("builtin:http.request", "io"), schema("url", "method")));
        // 添加条件判断组件。
        manifests.add(component("cmp-logic-condition", "logic.condition", "逻辑", "条件判断", "使用表达式输出 true/false 分支",
                "condition", ports(inputs(port("in", "输入", "object")), outputs(port("true", "满足", "object"), port("false", "不满足", "object"))),
                runtime("builtin:logic.condition", "logic"), schema("expression")));
        // 添加字段映射组件。
        manifests.add(component("cmp-transform-mapper", "transform.mapper", "转换", "字段映射", "用映射配置生成新的 payload",
                "mapper", ports(inputs(port("in", "输入", "object")), outputs(port("out", "输出", "object"))),
                runtime("builtin:transform.mapper", "transform"), schema("mapping")));
        // 添加系统日志组件。
        manifests.add(component("cmp-system-log", "system.log", "系统", "日志", "记录调试日志",
                "log", ports(inputs(port("in", "输入", "object")), outputs(port("out", "输出", "object"))),
                runtime("builtin:system.log", "system"), schema("message")));
        // 添加系统结束组件。
        manifests.add(component("cmp-system-end", "system.end", "系统", "结束", "流程结束节点",
                "end", ports(inputs(port("in", "输入", "object")), noOutputs()), runtime("builtin:system.end", "system"), objectSchema()));
        // 返回系统内置节点组件清单。
        return manifests;
    }

    /**
     * 创建节点组件清单。
     *
     * @param id 组件标识
     * @param type 组件类型
     * @param category 组件分类
     * @param name 组件名称
     * @param description 组件描述
     * @param icon 组件图标
     * @param ports 组件端口
     * @param runtime 组件运行时
     * @param schema 配置结构
     * @return 节点组件清单
     */
    private NodeComponentManifest component(String id,
                                            String type,
                                            String category,
                                            String name,
                                            String description,
                                            String icon,
                                            ComponentPorts ports,
                                            ComponentRuntime runtime,
                                            Map<String, Object> schema) {
        // 创建节点组件清单对象。
        NodeComponentManifest manifest = new NodeComponentManifest();
        // 主键由数据库自增生成。
        manifest.setId(null);
        // 写入组件类型。
        manifest.setType(type);
        // 写入默认组件版本。
        manifest.setVersion("1.0.0");
        // 写入组件分类。
        manifest.setCategory(category);
        // 写入组件名称。
        manifest.setName(name);
        // 写入组件描述。
        manifest.setDescription(description);
        // 写入组件图标。
        manifest.setIcon(icon);
        // 设置内置组件来源。
        manifest.setSource(NodeComponentSource.BUILTIN.getCode());
        // 写入组件端口。
        manifest.setPorts(ports);
        // 写入组件运行时。
        manifest.setRuntime(runtime);
        // 写入组件配置结构。
        manifest.setConfigSchema(schema);
        // 设置内置组件启用状态。
        manifest.setStatus(NodeComponentStatus.ACTIVE.getCode());
        // 写入组件排序号。
        manifest.setSortNo(0);
        // 返回节点组件清单。
        return manifest;
    }

    /**
     * 创建组件运行时定义。
     *
     * @param executor 执行器标识
     * @param capabilities 能力列表
     * @return 组件运行时定义
     */
    private ComponentRuntime runtime(String executor, String... capabilities) {
        // 创建组件运行时对象。
        ComponentRuntime runtime = new ComponentRuntime();
        // 写入执行器标识。
        runtime.setExecutor(executor);
        // 写入默认运行模式。
        runtime.setMode(Arrays.asList("debug", "production"));
        // 写入能力列表。
        runtime.setCapabilities(Arrays.asList(capabilities));
        // 返回组件运行时定义。
        return runtime;
    }

    /**
     * 创建组件端口集合。
     *
     * @param inputs 输入端口列表
     * @param outputs 输出端口列表
     * @return 组件端口集合
     */
    private ComponentPorts ports(List<ComponentPort> inputs, List<ComponentPort> outputs) {
        // 返回组件端口集合。
        return new ComponentPorts(inputs, outputs);
    }

    /**
     * 创建组件端口。
     *
     * @param id 端口标识
     * @param label 端口名称
     * @param schema 端口结构
     * @return 组件端口
     */
    private ComponentPort port(String id, String label, String schema) {
        // 返回非必填组件端口。
        return new ComponentPort(id, label, schema, false);
    }

    /**
     * 创建输入端口列表。
     *
     * @param ports 输入端口数组
     * @return 输入端口列表
     */
    private List<ComponentPort> inputs(ComponentPort... ports) {
        // 返回输入端口列表。
        return Arrays.asList(ports);
    }

    /**
     * 创建输出端口列表。
     *
     * @param ports 输出端口数组
     * @return 输出端口列表
     */
    private List<ComponentPort> outputs(ComponentPort... ports) {
        // 返回输出端口列表。
        return Arrays.asList(ports);
    }

    /**
     * 创建空输入端口列表。
     *
     * @return 空输入端口列表
     */
    private List<ComponentPort> noInputs() {
        // 返回空输入端口列表。
        return new ArrayList<>();
    }

    /**
     * 创建空输出端口列表。
     *
     * @return 空输出端口列表
     */
    private List<ComponentPort> noOutputs() {
        // 返回空输出端口列表。
        return new ArrayList<>();
    }

    /**
     * 创建对象类型配置结构。
     *
     * @param fields 字段名称数组
     * @return 配置结构
     */
    private Map<String, Object> schema(String... fields) {
        // 创建对象结构。
        Map<String, Object> schema = objectSchema();
        // 创建字段属性集合。
        Map<String, Object> properties = new LinkedHashMap<>();
        // 遍历字段名称数组。
        for (String field : fields) {
            // 创建字段结构。
            Map<String, Object> property = new LinkedHashMap<>();
            // 写入字段类型。
            property.put("type", "string");
            // 写入字段标题。
            property.put("title", field);
            // 写入字段属性集合。
            properties.put(field, property);
        }
        // 写入属性集合。
        schema.put("properties", properties);
        // 写入必填字段集合。
        schema.put("required", Arrays.asList(fields));
        // 返回配置结构。
        return schema;
    }

    /**
     * 创建基础对象结构。
     *
     * @return 基础对象结构
     */
    private Map<String, Object> objectSchema() {
        // 创建对象结构。
        Map<String, Object> schema = new LinkedHashMap<>();
        // 写入对象类型。
        schema.put("type", "object");
        // 返回对象结构。
        return schema;
    }
}
