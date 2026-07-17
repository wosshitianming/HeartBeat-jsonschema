package top.kx.heartbeat.infrastructure.persistence.example;

import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ExampleQuery {

    private ExampleQuery() {
    }

    public static <E, C> ExampleBuilder<E, C> of(E example, Class<C> criteriaType) {
        return new ExampleBuilder<>(example, criteriaType);
    }

    public static <E, C> C criteria(E example, Class<C> criteriaType) {
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(criteriaType, "criteriaType");
        Object criteria = invoke(example, "createCriteria");
        return criteriaType.cast(criteria);
    }

    public static <E> E orderBy(E example, OrderBy orderBy) {
        Objects.requireNonNull(orderBy, "orderBy");
        return orderBy(example, orderBy.toClause());
    }

    public static <E> E orderBy(E example, String orderByClause) {
        Objects.requireNonNull(example, "example");
        if (StringUtils.isNotBlank(orderByClause)) {
            invoke(example, "setOrderByClause", new Class<?>[]{String.class}, new Object[]{orderByClause.trim()});
        }
        return example;
    }

    public static <E> E distinct(E example) {
        Objects.requireNonNull(example, "example");
        invoke(example, "setDistinct", new Class<?>[]{boolean.class}, new Object[]{true});
        return example;
    }

    public static <E> E clear(E example) {
        Objects.requireNonNull(example, "example");
        invoke(example, "clear");
        return example;
    }

    public static <C> C apply(C criteria, Consumer<C> consumer) {
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(consumer, "consumer").accept(criteria);
        return criteria;
    }

    public static <C, V> C whenPresent(C criteria, V value, BiConsumer<C, V> consumer) {
        Objects.requireNonNull(criteria, "criteria");
        if (value != null) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, value);
        }
        return criteria;
    }

    public static <C> C whenNotBlank(C criteria, String value, BiConsumer<C, String> consumer) {
        Objects.requireNonNull(criteria, "criteria");
        if (StringUtils.isNotBlank(value)) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, value.trim());
        }
        return criteria;
    }

    public static <C, V> C whenNotEmpty(C criteria, Collection<V> values, BiConsumer<C, List<V>> consumer) {
        Objects.requireNonNull(criteria, "criteria");
        if (values != null && !values.isEmpty()) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, new ArrayList<>(values));
        }
        return criteria;
    }

    public static String containsLike(String value) {
        return "%" + nullToEmpty(value).trim() + "%";
    }

    public static String startsLike(String value) {
        return nullToEmpty(value).trim() + "%";
    }

    public static String endsLike(String value) {
        return "%" + nullToEmpty(value).trim();
    }

    public static <T> Optional<T> first(List<T> rows) {
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    public static <T> T firstOrNull(List<T> rows) {
        return first(rows).orElse(null);
    }

    public static <T> T requiredFirst(List<T> rows, Supplier<? extends RuntimeException> exceptionSupplier) {
        return first(rows).orElseThrow(exceptionSupplier);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Object invoke(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0], new Object[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 提取请求元数据，补齐日志中的访问来源信息。
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            // 返回已经完成封装的业务结果。
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Throwable targetException = ex.getTargetException();
            // 根据当前业务条件选择对应处理路径。
            if (targetException instanceof RuntimeException) {
                // 对非法业务状态立即失败，避免错误继续扩散。
                throw (RuntimeException) targetException;
            }
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("Failed to invoke Example method: " + methodName, targetException);
        } catch (ReflectiveOperationException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException(
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    "Unsupported MyBatis Generator Example type: " + target.getClass().getName(), ex);
        }
    }
}
