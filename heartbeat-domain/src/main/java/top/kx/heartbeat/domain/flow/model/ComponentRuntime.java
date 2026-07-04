package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点组件运行时模型。
 *
 * <p>用于描述组件对应的执行器、运行模式和能力声明。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRuntime {

    /**
     * 执行器标识。
     */
    private String executor;

    /**
     * 运行模式列表。
     */
    private List<String> mode = new ArrayList<>();

    /**
     * 运行能力列表。
     */
    private List<String> capabilities = new ArrayList<>();
}
