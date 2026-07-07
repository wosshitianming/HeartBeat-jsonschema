package top.kx.heartbeat.interfaces.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.structure.StructureErrorCode;
import top.kx.heartbeat.domain.user.UserErrorCode;

import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>负责将领域异常、请求异常、权限异常和未知异常翻译为统一响应协议。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理领域异常。
     *
     * @param ex 领域异常
     * @param response HTTP 响应对象
     * @return 失败响应对象
     */
    @ExceptionHandler(DomainException.class)
    public Result<Void> handleDomain(DomainException ex, HttpServletResponse response) {
        // 记录领域规则失败日志。
        log.warn("领域规则校验失败, code={}, msg={}", ex.getCode(), ex.getMessage());
        // 映射领域错误码对应 HTTP 状态。
        HttpStatus status = mapStatus(ex.getCode());
        // 写入 HTTP 响应状态。
        response.setStatus(status.value());
        // 返回统一失败响应。
        return Result.failure(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * @param ex 参数校验异常
     * @return 失败响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        // 拼接字段校验错误消息。
        String msg = ex.getBindingResult().getFieldErrors().stream()
                // 格式化单个字段错误。
                .map(this::formatFieldError)
                // 使用分号连接所有字段错误。
                .collect(Collectors.joining("; "));
        // 返回请求参数失败响应。
        return Result.failure("REQUEST_INVALID", msg);
    }

    /**
     * 处理请求体不可读异常。
     *
     * @param ex 请求体不可读异常
     * @return 失败响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        // 返回 JSON 格式错误响应。
        return Result.failure("REQUEST_INVALID", "请求 JSON 格式或枚举值不正确");
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @return 失败响应对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        // 返回非法参数失败响应。
        return Result.failure("REQUEST_INVALID", ex.getMessage());
    }

    /**
     * 处理访问拒绝异常。
     *
     * @param ex 访问拒绝异常
     * @return 失败响应对象
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        // 返回权限不足失败响应。
        return Result.failure("ACCESS_DENIED", "无权执行该操作");
    }

    /**
     * 处理未知异常。
     *
     * @param ex 未知异常
     * @return 失败响应对象
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpected(Exception ex) {
        // 记录未知异常堆栈。
        log.error("系统未知异常", ex);
        // 返回系统繁忙失败响应。
        return Result.failure("INTERNAL_ERROR", "系统繁忙，请稍后再试");
    }

    /**
     * 映射领域错误码到 HTTP 状态。
     *
     * @param code 领域错误码
     * @return HTTP 状态
     */
    private HttpStatus mapStatus(String code) {
        // 判断用户不存在错误码。
        if (UserErrorCode.USER_NOT_FOUND.equals(code)) {
            // 用户不存在映射为 404。
            return HttpStatus.NOT_FOUND;
        }
        // 判断结构资源不存在错误码。
        if (StructureErrorCode.STRUCTURE_NOT_FOUND.equals(code)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                || StructureErrorCode.STRUCTURE_VERSION_NOT_FOUND.equals(code)) {
            // 结构资源不存在映射为 404。
            return HttpStatus.NOT_FOUND;
        }
        // 判断邮箱重复错误码。
        if (UserErrorCode.EMAIL_DUPLICATED.equals(code)) {
            // 邮箱重复映射为 409。
            return HttpStatus.CONFLICT;
        }
        // 默认业务语义异常映射为 422。
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    /**
     * 格式化字段校验错误。
     *
     * @param error 字段校验错误
     * @return 字段校验错误消息
     */
    private String formatFieldError(FieldError error) {
        // 拼接字段名称与默认错误消息。
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
