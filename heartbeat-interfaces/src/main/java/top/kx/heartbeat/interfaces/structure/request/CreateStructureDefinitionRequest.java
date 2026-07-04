package top.kx.heartbeat.interfaces.structure.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import top.kx.heartbeat.application.structure.dto.ValidationMode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建结构定义请求对象。
 *
 * <p>用于接收结构定义首次创建时的样本、校验模式和界面覆盖配置。</p>
 */
@Getter
@Setter
public class CreateStructureDefinitionRequest {

    /**
     * 结构定义名称。
     */
    @NotBlank(message = "name 不能为空")
    private String name;

    /**
     * 结构定义描述。
     */
    private String description;

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

    /**
     * 是否创建后立即激活。
     */
    private boolean activate = true;
}
