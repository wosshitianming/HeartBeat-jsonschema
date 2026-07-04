package top.kx.heartbeat.application.flow.runtime;


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.flow.model.FlowNodeRunStatus;

import java.util.*;

/**
 * 内置节点执行器工厂。
 *
 * <p>负责提供流程运行时默认可用的基础节点执行器集合。</p>
 */
@Component
public class BuiltinNodeExecutors {

    /**
     * 查询全部内置节点执行器。
     *
     * @return 内置节点执行器列表
     */
    public List<NodeExecutor> all() {
        // 创建节点执行器列表。
        List<NodeExecutor> executors = new ArrayList<>();
        // 注册手动触发执行器。
        executors.add(simple("builtin:trigger.manual", "out"));
        // 注册 Webhook 触发执行器。
        executors.add(simple("builtin:trigger.webhook", "out"));
        // 注册 MySQL 查询执行器。
        executors.add(simple("builtin:mysql.query", "out", "mockRows"));
        // 注册 Redis 读取执行器。
        executors.add(simple("builtin:redis.get", "out", "mockValue"));
        // 注册 Redis 写入执行器。
        executors.add(simple("builtin:redis.set", "out"));
        // 注册消息消费执行器。
        executors.add(simple("builtin:mq.consume", "out"));
        // 注册消息发布执行器。
        executors.add(simple("builtin:mq.publish", "out"));
        // 注册 HTTP 请求执行器。
        executors.add(simple("builtin:http.request", "out", "mockResponse"));
        // 注册条件判断执行器。
        executors.add(condition());
        // 注册字段映射执行器。
        executors.add(mapper());
        // 注册系统日志执行器。
        executors.add(simple("builtin:system.log", "out"));
        // 注册系统结束执行器。
        executors.add(simple("builtin:system.end"));
        // 返回节点执行器列表。
        return executors;
    }

    /**
     * 创建透传型节点执行器。
     *
     * @param id 执行器标识
     * @param nextPorts 后续端口列表
     * @return 节点执行器
     */
    private NodeExecutor simple(String id, String... nextPorts) {
        // 返回匿名透传节点执行器。
        return new NodeExecutor() {
            @Override
            public String executorId() {
                // 返回执行器标识。
                return id;
            }

            @Override
            public NodeExecutionResult execute(NodeExecutionContext context) {
                // 复制当前节点输入作为输出。
                Map<String, Object> output = new LinkedHashMap<>(context.getInput());
                // 判断是否需要追加模拟输出字段。
                if (nextPorts.length > 1) {
                    // 写入模拟输出字段。
                    output.put(nextPorts[1], true);
                }
                // 计算后续端口列表。
                List<String> ports = nextPorts.length == 0
                        ? Collections.emptyList()
                        : Collections.singletonList(nextPorts[0]);
                // 返回节点执行成功结果。
                return new NodeExecutionResult(FlowNodeRunStatus.SUCCESS.getCode(), output, ports, null);
            }
        };
    }

    /**
     * 创建条件判断节点执行器。
     *
     * @return 条件判断节点执行器
     */
    private NodeExecutor condition() {
        // 返回匿名条件判断节点执行器。
        return new NodeExecutor() {
            @Override
            public String executorId() {
                // 返回条件判断执行器标识。
                return "builtin:logic.condition";
            }

            @Override
            public NodeExecutionResult execute(NodeExecutionContext context) {
                // 读取条件表达式。
                String expression = String.valueOf(context.getNode().getConfig().getOrDefault("expression", ""));
                // 执行等值表达式判断。
                boolean matched = evaluateEqualsExpression(expression, context.getInput());
                // 返回条件判断执行结果。
                return new NodeExecutionResult(
                        FlowNodeRunStatus.SUCCESS.getCode(),
                        new LinkedHashMap<>(context.getInput()),
                        Collections.singletonList(matched ? "true" : "false"),
                        null
                );
            }
        };
    }

    /**
     * 创建字段映射节点执行器。
     *
     * @return 字段映射节点执行器
     */
    @SuppressWarnings("unchecked")
    private NodeExecutor mapper() {
        // 返回匿名字段映射节点执行器。
        return new NodeExecutor() {
            @Override
            public String executorId() {
                // 返回字段映射执行器标识。
                return "builtin:transform.mapper";
            }

            @Override
            public NodeExecutionResult execute(NodeExecutionContext context) {
                // 读取字段映射配置。
                Object mappingObject = context.getNode().getConfig().get("mapping");
                // 判断字段映射配置是否有效。
                if (!(mappingObject instanceof Map)) {
                    // 无有效映射配置时透传输入。
                    return new NodeExecutionResult(
                            FlowNodeRunStatus.SUCCESS.getCode(),
                            new LinkedHashMap<>(context.getInput()),
                            Collections.singletonList("out"),
                            null
                    );
                }
                // 创建字段映射输出。
                Map<String, Object> output = new LinkedHashMap<>();
                // 转换字段映射配置。
                Map<String, Object> mapping = (Map<String, Object>) mappingObject;
                // 遍历字段映射规则。
                for (Map.Entry<String, Object> entry : mapping.entrySet()) {
                    // 写入映射后的字段值。
                    output.put(entry.getKey(), resolvePath(context.getInput(), String.valueOf(entry.getValue())));
                }
                // 返回字段映射执行结果。
                return new NodeExecutionResult(FlowNodeRunStatus.SUCCESS.getCode(), output, Collections.singletonList("out"), null);
            }
        };
    }

    /**
     * 计算等值表达式。
     *
     * @param expression 条件表达式
     * @param input 节点输入数据
     * @return 是否命中表达式
     */
    private boolean evaluateEqualsExpression(String expression, Map<String, Object> input) {
        // 标准化表达式文本。
        String expr = expression == null ? "" : expression.trim();
        // 识别表达式操作符。
        String operator = expr.contains("==") ? "==" : (expr.contains("!=") ? "!=" : "");
        // 判断操作符是否存在。
        if (StringUtils.isEmpty(operator)) {
            // 无操作符时表达式不匹配。
            return false;
        }
        // 拆分表达式左右两侧。
        String[] parts = expr.split(operator, 2);
        // 判断表达式结构是否有效。
        if (parts.length != 2) {
            // 表达式结构无效时不匹配。
            return false;
        }
        // 解析实际值。
        Object actual = resolvePath(input, parts[0].trim());
        // 解析期望值。
        String expected = parts[1].trim().replace("'", "").replace("\"", "");
        // 比较实际值与期望值。
        boolean equals = String.valueOf(actual).equals(expected);
        // 根据操作符返回判断结果。
        return "==".equals(operator) ? equals : !equals;
    }

    /**
     * 解析输入字段路径。
     *
     * @param input 节点输入数据
     * @param path 字段路径
     * @return 字段路径对应值
     */
    private Object resolvePath(Map<String, Object> input, String path) {
        // 标准化字段路径。
        String normalized = path.replace("payload.", "").replace("$.", "").trim();
        // 判断输入中是否存在该字段。
        if (input.containsKey(normalized)) {
            // 返回输入字段值。
            return input.get(normalized);
        }
        // 返回原始路径文本作为兜底值。
        return normalized;
    }
}
