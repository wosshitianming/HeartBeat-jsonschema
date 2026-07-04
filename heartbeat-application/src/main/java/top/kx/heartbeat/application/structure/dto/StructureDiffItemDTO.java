package top.kx.heartbeat.application.structure.dto;

import lombok.Value;

/**
 * 结构差异项数据传输对象。
 *
 * <p>用于描述单个结构路径上的变化。</p>
 */
@Value
public class StructureDiffItemDTO {
    /**
     * 差异分类。
     */
    String category;
    /**
     * 差异路径。
     */
    String path;
    /**
     * 差异类型。
     */
    String changeType;
    /**
     * 变更前内容。
     */
    String before;
    /**
     * 变更后内容。
     */
    String after;
}
