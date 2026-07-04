package top.kx.heartbeat.domain.common;

/**
 * 值对象标记接口。
 *
 * <p>值对象没有唯一标识，通过其全部属性判断相等，必须是不可变的（创建后不可修改）。
 * 实现类应重写 {@code equals}/{@code hashCode}（推荐使用 record 或 Lombok 的 {@code @Value}）。
 */
public interface ValueObject {
}
