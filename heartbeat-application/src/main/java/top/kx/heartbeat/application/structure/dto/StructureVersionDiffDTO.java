package top.kx.heartbeat.application.structure.dto;

import lombok.Value;

import java.util.List;

/**
 * 结构版本差异数据传输对象。
 *
 * <p>用于返回两个结构版本之间的差异列表。</p>
 */
@Value
public class StructureVersionDiffDTO {
    /**
     * 结构定义标识。
     */
    String definitionId;
    /**
     * 源版本号。
     */
    Integer fromVersionNo;
    /**
     * 目标版本号。
     */
    Integer toVersionNo;
    /**
     * 目标是否为草稿。
     */
    boolean toDraft;
    /**
     * 差异项列表。
     */
    List<StructureDiffItemDTO> changes;
}
