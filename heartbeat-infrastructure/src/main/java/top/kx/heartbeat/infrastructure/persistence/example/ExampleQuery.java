package top.kx.heartbeat.infrastructure.persistence.example;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        if (orderByClause != null && !orderByClause.trim().isEmpty()) {
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
        if (value != null && !value.trim().isEmpty()) {
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
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            throw new IllegalStateException("Failed to invoke Example method: " + methodName, targetException);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "Unsupported MyBatis Generator Example type: " + target.getClass().getName(), ex);
        }
    }
}
