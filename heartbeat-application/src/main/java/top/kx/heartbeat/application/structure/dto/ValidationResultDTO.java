package top.kx.heartbeat.application.structure.dto;

import lombok.Value;

import java.util.List;

/**
 * 结构数据校验结果数据传输对象。
 *
 * <p>用于返回结构校验是否通过及错误明细。</p>
 */
@Value
public class ValidationResultDTO {
    /**
     * 是否校验通过。
     */
    boolean valid;
    /**
     * 结构定义标识。
     */
    String definitionId;
    /**
     * 校验使用的版本号。
     */
    int versionNo;
    /**
     * 校验使用的模式。
     */
    ValidationMode mode;
    /**
     * 校验错误列表。
     */
    List<ValidationError> errors;
}
