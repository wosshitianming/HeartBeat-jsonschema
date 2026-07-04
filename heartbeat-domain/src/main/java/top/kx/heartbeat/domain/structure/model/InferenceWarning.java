package top.kx.heartbeat.domain.structure.model;

import lombok.Value;

/**
 * 结构推断过程中的非阻断警告。
 */
@Value
public class InferenceWarning {
    String code;
    String path;
    String message;
}
