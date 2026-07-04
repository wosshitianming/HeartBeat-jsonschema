package top.kx.heartbeat.infrastructure.persistence.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class OrderBy {

    private static final Pattern COLUMN_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final List<String> segments;

    private OrderBy(List<String> segments) {
        this.segments = Collections.unmodifiableList(segments);
    }

    public static OrderBy asc(String column) {
        return new OrderBy(Collections.singletonList(segment(column, Direction.ASC)));
    }

    public static OrderBy desc(String column) {
        return new OrderBy(Collections.singletonList(segment(column, Direction.DESC)));
    }

    public OrderBy thenAsc(String column) {
        return then(column, Direction.ASC);
    }

    public OrderBy thenDesc(String column) {
        return then(column, Direction.DESC);
    }

    public OrderBy then(String column, Direction direction) {
        List<String> next = new ArrayList<>(segments);
        next.add(segment(column, direction));
        return new OrderBy(next);
    }

    public String toClause() {
        return String.join(", ", segments);
    }

    @Override
    public String toString() {
        return toClause();
    }

    private static String segment(String column, Direction direction) {
        if (column == null || !COLUMN_PATTERN.matcher(column.trim()).matches()) {
            throw new IllegalArgumentException("Invalid order column: " + column);
        }
        return column.trim() + " " + direction.name();
    }

    public enum Direction {
        ASC,
        DESC
    }
}
