package top.kx.heartbeat.interfaces.common;

import lombok.Getter;

import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * <p>用于承接接口层对外返回的稳定协议结构。</p>
 *
 * @param <T> 响应数据类型
 */
@Getter
public class Result<T> implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 成功响应编码。
     */
    private static final String SUCCESS_CODE = "0";

    /**
     * 成功响应消息。
     */
    private static final String SUCCESS_MSG = "success";

    /**
     * 响应编码。
     */
    private final String code;

    /**
     * 响应消息。
     */
    private final String msg;

    /**
     * 响应数据。
     */
    private final T data;

    /**
     * 创建统一接口响应对象。
     *
     * @param code 响应编码
     * @param msg 响应消息
     * @param data 响应数据
     */
    private Result(String code, String msg, T data) {
        // 绑定响应编码。
        this.code = code;
        // 绑定响应消息。
        this.msg = msg;
        // 绑定响应数据。
        this.data = data;
    }

    /**
     * 创建带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        // 返回带数据的成功响应。
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    /**
     * 创建空成功响应。
     *
     * @return 空成功响应对象
     */
    public static Result<Void> success() {
        // 返回空数据成功响应。
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    /**
     * 创建失败响应。
     *
     * @param code 失败编码
     * @param msg 失败消息
     * @param <T> 响应数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> failure(String code, String msg) {
        // 返回失败响应。
        return new Result<>(code, msg, null);
    }
}
