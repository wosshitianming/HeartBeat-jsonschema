package top.kx.heartbeat.application.admin.dto;

import lombok.Value;

/**
 * 后台模块指标数据传输对象。
 *
 * <p>用于展示后台模块的补充统计或说明信息。</p>
 */
@Value
public class AdminMetricDTO {

    /**
     * 指标展示名称。
     */
    String label;

    /**
     * 指标展示值。
     */
    String value;

    /**
     * 指标提示文案。
     */
    String hint;
}
