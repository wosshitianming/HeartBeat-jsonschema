package top.kx.heartbeat.infrastructure.persistence.example;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ExampleBuilder<E, C> {

    private final E example;
    private final C criteria;

    ExampleBuilder(E example, Class<C> criteriaType) {
        this.example = Objects.requireNonNull(example, "example");
        this.criteria = ExampleQuery.criteria(example, criteriaType);
    }

    public ExampleBuilder<E, C> where(Consumer<C> consumer) {
        Objects.requireNonNull(consumer, "consumer").accept(criteria);
        return this;
    }

    public <V> ExampleBuilder<E, C> whenPresent(V value, BiConsumer<C, V> consumer) {
        if (value != null) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, value);
        }
        return this;
    }

    public ExampleBuilder<E, C> whenNotBlank(String value, BiConsumer<C, String> consumer) {
        if (StringUtils.isNotBlank(value)) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, value.trim());
        }
        return this;
    }

    public <V> ExampleBuilder<E, C> whenNotEmpty(Collection<V> values, BiConsumer<C, List<V>> consumer) {
        if (values != null && !values.isEmpty()) {
            Objects.requireNonNull(consumer, "consumer").accept(criteria, new ArrayList<>(values));
        }
        return this;
    }

    public ExampleBuilder<E, C> orderBy(OrderBy orderBy) {
        ExampleQuery.orderBy(example, orderBy);
        return this;
    }

    public ExampleBuilder<E, C> orderBy(String orderByClause) {
        ExampleQuery.orderBy(example, orderByClause);
        return this;
    }

    public ExampleBuilder<E, C> distinct() {
        ExampleQuery.distinct(example);
        return this;
    }

    public C criteria() {
        return criteria;
    }

    public E build() {
        return example;
    }
}
