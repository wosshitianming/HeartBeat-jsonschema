package top.kx.heartbeat.interfaces.structure.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import top.kx.heartbeat.application.structure.dto.ValidationMode;

import javax.validation.constraints.NotNull;

/**
 * 校验结构数据请求对象。
 *
 * <p>用于按照指定结构版本和校验模式校验业务数据。</p>
 */
@Getter
@Setter
public class ValidateStructureRequest {

    /**
     * 目标结构版本号。
     */
    private Integer versionNo;

    /**
     * 结构数据校验模式。
     */
    private ValidationMode validationMode;

    /**
     * 待校验业务数据。
     */
    @NotNull(message = "payload 不能为空")
    private JsonNode payload;
}
