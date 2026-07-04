package top.kx.heartbeat.domain.user.model.valueobject;


import org.apache.commons.lang3.StringUtils;
import top.kx.heartbeat.domain.common.ValueObject;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.user.UserErrorCode;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 邮箱值对象。
 *
 * <p>把"格式合法"这一不变量内聚在值对象的构造过程中：一旦创建成功，系统中就不存在非法邮箱。
 * 这避免了校验逻辑散落在各层，是 DDD"让非法状态无法表达"的典型实践。
 */
public final class Email implements ValueObject {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new DomainException(UserErrorCode.EMAIL_INVALID, "邮箱不能为空");
        }
        String normalized = raw.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new DomainException(UserErrorCode.EMAIL_INVALID, "邮箱格式不合法: " + raw);
        }
        return new Email(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
