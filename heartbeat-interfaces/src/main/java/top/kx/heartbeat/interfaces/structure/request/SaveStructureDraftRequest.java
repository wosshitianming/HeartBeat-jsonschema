package top.kx.heartbeat.interfaces.structure.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import top.kx.heartbeat.application.structure.dto.ValidationMode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 保存结构草稿请求对象。
 *
 * <p>用于保存结构定义版本的草稿样本和字段覆盖配置。</p>
 */
@Getter
@Setter
public class SaveStructureDraftRequest {

    /**
     * 草稿推断使用的样本数据列表。
     */
    @NotEmpty(message = "samples 不能为空")
    private List<JsonNode> samples;

    /**
     * 结构推断校验模式。
     */
    private ValidationMode validationMode = ValidationMode.LENIENT;

    /**
     * 字段配置覆盖项。
     */
    private JsonNode fieldOverrides;

    /**
     * 获取兼容旧字段命名的界面覆盖项。
     *
     * @return 字段配置覆盖项
     */
    public JsonNode getUiOverrides() {
        // 返回字段覆盖配置作为界面覆盖配置。
        return fieldOverrides;
    }
}
