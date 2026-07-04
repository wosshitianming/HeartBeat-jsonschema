package top.kx.heartbeat.interfaces.structure.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 激活结构版本请求对象。
 *
 * <p>用于指定需要激活的结构版本号。</p>
 */
@Getter
@Setter
public class ActivateStructureVersionRequest {

    /**
     * 需要激活的版本号。
     */
    @NotNull(message = "versionNo 不能为空")
    @Min(value = 1, message = "versionNo 必须大于 0")
    private Integer versionNo;
}
