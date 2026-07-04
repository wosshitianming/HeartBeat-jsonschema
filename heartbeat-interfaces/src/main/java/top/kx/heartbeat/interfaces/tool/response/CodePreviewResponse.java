package top.kx.heartbeat.interfaces.tool.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成代码预览响应对象。
 *
 * <p>用于替代接口层直接返回 {@code Map<String, String>}，让预览结果拥有明确的响应语义。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePreviewResponse {

    /**
     * 生成文件路径与文件内容映射。
     */
    private Map<String, String> files;

    /**
     * 将生成代码文件映射转换为响应对象。
     *
     * @param files 生成文件路径与文件内容映射
     * @return 生成代码预览响应对象
     */
    public static CodePreviewResponse from(Map<String, String> files) {
        // 兜底空文件映射。
        Map<String, String> safeFiles = files == null ? Collections.emptyMap() : files;
        // 复制文件映射，避免外部继续修改响应对象内部状态。
        Map<String, String> copiedFiles = new LinkedHashMap<>(safeFiles);
        // 返回生成代码预览响应对象。
        return new CodePreviewResponse(copiedFiles);
    }
}
