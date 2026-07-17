package top.kx.heartbeat.application.flow.runtime;

import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;

/**
 * 由 Java 代码定义的流程节点。
 *
 * <p>实现类只需作为 Spring Bean 暴露，节点执行器和组件清单便会被同时发现。</p>
 */
public interface CodeNode extends NodeExecutor {

    /**
     * 返回画布、校验和运行时共用的组件清单。
     *
     * @return 节点组件清单
     */
    NodeComponentManifest manifest();
}
