package top.kx.heartbeat.infrastructure.persistence.audit;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;

import javax.annotation.Resource;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * 统一填充 MyBatis 实体的创建/更新审计字段。
 */
@Component
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class AuditFieldInterceptor implements Interceptor {

    /**
     * 系统默认用户标识。
     */
    private static final long SYSTEM_USER_ID = 0L;

    /**
     * 当前登录用户提供器。
     */
    @Resource
    private CurrentUserProvider currentUserProvider;

    /**
     * 拦截 MyBatis 的新增和更新操作，自动补齐审计字段。
     *
     * @param invocation MyBatis 拦截调用上下文。
     * @return 原 MyBatis 调用结果。
     * @throws Throwable 原调用或审计字段填充失败时抛出。
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 读取当前执行的 Mapper 语句。
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        // 判断 SQL 命令类型，仅处理 INSERT 和 UPDATE。
        SqlCommandType commandType = statement.getSqlCommandType();
        if (commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE) {
            // 审计时间在一次拦截中保持一致，避免批量对象时间漂移。
            LocalDateTime now = LocalDateTime.now();
            // 审计用户优先取当前登录用户，取不到时回落系统用户。
            long userId = currentUserId();
            // 对 Mapper 入参递归填充审计字段。
            fill(invocation.getArgs()[1], commandType, now, userId);
        }
        // 继续执行原 MyBatis 调用。
        return invocation.proceed();
    }

    /**
     * 包装 MyBatis 目标对象。
     *
     * @param target MyBatis 插件目标对象。
     * @return 包装后的目标对象。
     */
    @Override
    public Object plugin(Object target) {
        // 使用 MyBatis 标准插件包装器启用当前拦截器。
        return Plugin.wrap(target, this);
    }

    /**
     * 设置插件属性。
     *
     * @param properties MyBatis 插件配置属性。
     */
    @Override
    public void setProperties(java.util.Properties properties) {
        // 无需配置
    }

    /**
     * 递归填充审计字段。
     *
     * @param value Mapper 入参或入参中的嵌套对象。
     * @param commandType SQL 命令类型。
     * @param now 当前审计时间。
     * @param userId 当前审计用户标识。
     */
    private void fill(Object value, SqlCommandType commandType, LocalDateTime now, long userId) {
        // 空入参无需处理。
        if (value == null) {
            return;
        }
        // MyBatis 多参数场景通常会包装成 Map，需要递归处理每个参数值。
        if (value instanceof Map) {
            for (Object item : ((Map<?, ?>) value).values()) {
                fill(item, commandType, now, userId);
            }
            return;
        }
        // 批量插入或批量更新的集合参数逐项处理。
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                fill(item, commandType, now, userId);
            }
            return;
        }
        // 数组参数逐项处理。
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                fill(Array.get(value, index), commandType, now, userId);
            }
            return;
        }

        // 使用 MyBatis MetaObject 以反射方式访问实体属性。
        MetaObject metaObject = SystemMetaObject.forObject(value);
        if (commandType == SqlCommandType.INSERT) {
            // 新增时只在创建字段为空时填充，避免覆盖业务显式指定的创建信息。
            setIfNull(metaObject, "createTime", auditTimeValue(metaObject, "createTime", now));
            setIfNull(metaObject, "createBy", auditUserValue(metaObject, "createBy", userId));
        }
        // 新增和更新都刷新更新时间与更新人。
        set(metaObject, "updateTime", auditTimeValue(metaObject, "updateTime", now));
        set(metaObject, "updateBy", auditUserValue(metaObject, "updateBy", userId));
    }

    /**
     * 当目标属性存在且为空时写入字段值。
     *
     * @param metaObject MyBatis 元对象。
     * @param property 属性名。
     * @param value 待写入值。
     */
    private void setIfNull(MetaObject metaObject, String property, Object value) {
        // 只有存在 setter，且没有 getter 或当前值为空时才写入。
        if (metaObject.hasSetter(property)
                && (!metaObject.hasGetter(property) || metaObject.getValue(property) == null)) {
            metaObject.setValue(property, value);
        }
    }

    /**
     * 当目标属性存在时写入字段值。
     *
     * @param metaObject MyBatis 元对象。
     * @param property 属性名。
     * @param value 待写入值。
     */
    private void set(MetaObject metaObject, String property, Object value) {
        // 实体没有该属性时直接跳过，兼容不同表结构。
        if (metaObject.hasSetter(property)) {
            metaObject.setValue(property, value);
        }
    }

    /**
     * 根据实体字段类型生成审计用户值。
     *
     * @param metaObject MyBatis 元对象。
     * @param property 属性名。
     * @param userId 当前用户标识。
     * @return 与字段类型匹配的用户值。
     */
    private Object auditUserValue(MetaObject metaObject, String property, long userId) {
        // 没有 setter 时返回空值，由调用方跳过写入。
        if (!metaObject.hasSetter(property)) {
            return null;
        }
        // createBy/updateBy 有些实体是 String，有些是 Long，这里做类型适配。
        Class<?> propertyType = metaObject.getSetterType(property);
        return propertyType == String.class ? String.valueOf(userId) : userId;
    }

    /**
     * 根据实体字段类型生成审计时间值。
     *
     * @param metaObject MyBatis 元对象。
     * @param property 属性名。
     * @param now 当前审计时间。
     * @return 与字段类型匹配的时间值。
     */
    private Object auditTimeValue(MetaObject metaObject, String property, LocalDateTime now) {
        // 没有 setter 时返回空值，由调用方跳过写入。
        if (!metaObject.hasSetter(property)) {
            return null;
        }
        // 兼容旧实体 Date 字段和新实体 LocalDateTime 字段。
        Class<?> propertyType = metaObject.getSetterType(property);
        if (propertyType == Date.class) {
            return Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        }
        return now;
    }

    /**
     * 获取当前审计用户标识。
     *
     * @return 当前用户标识，未登录或解析失败时返回系统用户。
     */
    private long currentUserId() {
        // 从 Spring Security 上下文读取认证对象。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证、匿名用户或 principal 缺失时使用系统用户。
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return SYSTEM_USER_ID;
        }
        try {
            // 认证存在时使用领域端口解析当前用户标识。
            return Long.parseLong(currentUserProvider.currentUserId());
        } catch (RuntimeException ignored) {
            // 解析失败时兜底为系统用户，避免审计逻辑阻断业务 SQL。
            return SYSTEM_USER_ID;
        }
    }
}
