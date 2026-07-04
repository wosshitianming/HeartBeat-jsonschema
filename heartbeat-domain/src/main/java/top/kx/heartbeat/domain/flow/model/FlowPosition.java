package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程节点画布位置模型。
 *
 * <p>用于描述流程节点在画布中的二维坐标。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowPosition {

    /**
     * 横向坐标。
     */
    private int x;

    /**
     * 纵向坐标。
     */
    private int y;
}
