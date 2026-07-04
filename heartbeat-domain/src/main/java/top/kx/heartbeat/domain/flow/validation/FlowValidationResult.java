package top.kx.heartbeat.domain.flow.validation;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流程 DSL 校验结果。
 *
 * <p>用于承接流程定义校验过程中的问题列表与校验状态。</p>
 */
@Getter
public class FlowValidationResult {

    /**
     * 校验问题列表。
     */
    private final List<FlowValidationIssue> issues = new ArrayList<>();

    /**
     * 判断校验是否通过。
     *
     * @return 是否校验通过
     */
    public boolean isValid() {
        // 校验问题为空时表示通过。
        return CollectionUtils.isEmpty(issues);
    }

    /**
     * 获取不可变校验问题列表。
     *
     * @return 不可变校验问题列表
     */
    public List<FlowValidationIssue> getIssues() {
        // 返回不可变校验问题列表。
        return Collections.unmodifiableList(issues);
    }

    /**
     * 添加校验问题。
     *
     * @param code 问题编码
     * @param path 问题路径
     * @param message 问题消息
     */
    public void add(String code, String path, String message) {
        // 添加流程校验问题。
        issues.add(new FlowValidationIssue(code, path, message));
    }
}
