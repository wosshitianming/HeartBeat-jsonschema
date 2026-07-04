package top.kx.heartbeat.application.structure.dto;

import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * 结构定义数据传输对象。
 *
 * <p>用于返回结构定义、草稿和版本列表。</p>
 */
@Value
public class StructureDefinitionDTO {
    /**
     * 结构定义标识。
     */
    String id;
    /**
     * 结构定义名称。
     */
    String name;
    /**
     * 结构定义描述。
     */
    String description;
    /**
     * 当前激活版本号。
     */
    Integer activeVersionNo;
    /**
     * 结构定义状态编码。
     */
    String status;
    /**
     * 结构定义草稿。
     */
    StructureDraftDTO draft;
    /**
     * 结构定义版本列表。
     */
    List<StructureVersionDTO> versions;
    /**
     * 结构定义创建时间。
     */
    Instant createTime;
    /**
     * 结构定义更新时间。
     */
    Instant updateTime;
}
