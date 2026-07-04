package top.kx.heartbeat.interfaces.structure.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import top.kx.heartbeat.application.structure.dto.ValidationMode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 预览结构推断请求对象。
 *
 * <p>用于在不落库的情况下预览样本推断出的结构模型。</p>
 */
@Getter
@Setter
public class PreviewStructureRequest {

    /**
     * 推断结构使用的样本数据列表。
     */
    @NotEmpty(message = "samples 不能为空")
    private List<JsonNode> samples;

    /**
     * 结构推断校验模式。
     */
    private ValidationMode validationMode = ValidationMode.LENIENT;

    /**
     * 界面配置覆盖项。
     */
    private JsonNode uiOverrides;
}
