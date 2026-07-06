package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractPlatformRepositorySupport {

    protected <T> Optional<T> first(List<T> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    protected List<DomainRecord> records(List<?> rows) {
        return rows == null
                ? Collections.emptyList()
                : rows.stream().map(this::record).collect(Collectors.toList());
    }

    protected DomainRecord record(Object row) {
        return DomainRecord.of(toMap(row));
    }

    protected DomainRecord create(Object mapper, Object row, Map<String, Object> command) {
        applyCommand(row, command);
        touch(row, true);
        invoke(mapper, "insertSelective", row);
        return record(row);
    }

    protected DomainRecord update(Object mapper, Object row, String id, Map<String, Object> command) {
        set(row, "id", longValue(id));
        applyCommand(row, command);
        touch(row, false);
        invoke(mapper, "updateByPrimaryKeySelective", row);
        Object persisted = invoke(mapper, "selectByPrimaryKey", longValue(id));
        return record(persisted == null ? row : persisted);
    }

    protected void delete(Object mapper, String id) {
        invoke(mapper, "deleteByPrimaryKey", longValue(id));
    }

    protected Long longValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    protected Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    protected void touch(Object row, boolean creating) {
        Date now = new Date();
        if (creating) {
            setIfNull(row, "tenantId", tenantId());
            setIfNull(row, "createTime", now);
            setIfNull(row, "version", 0);
            setIfNull(row, "deleteMarker", 0L);
            setIfBlank(row, "status", "ENABLED");
        }
        set(row, "updateTime", now);
    }

    protected void set(Object row, String property, Object value) {
        String setter = "set" + capitalize(property);
        for (Method method : row.getClass().getMethods()) {
            if (!method.getName().equals(setter) || method.getParameterTypes().length != 1
                    || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            try {
                method.invoke(row, convert(value, method.getParameterTypes()[0]));
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            }
            return;
        }
    }

    private Map<String, Object> toMap(Object row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return values;
        }
        for (Method method : row.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getDeclaringClass() == Object.class) {
                continue;
            }
            String name = propertyName(method);
            if (name == null) {
                continue;
            }
            try {
                values.put(name, method.invoke(row));
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return values;
    }

    private void applyCommand(Object row, Map<String, Object> command) {
        set(row, "tenantId", tenantId());
        if (command == null) {
            return;
        }
        for (Method method : row.getClass().getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterTypes().length != 1) {
                continue;
            }
            String property = decapitalize(method.getName().substring(3));
            if ("id".equals(property) || "tenantId".equals(property)) {
                continue;
            }
            Object value = command.containsKey(property) ? command.get(property) : command.get(camelToSnake(property));
            if (value != null) {
                set(row, property, value);
            }
        }
    }

    private void setIfNull(Object row, String property, Object value) {
        if (get(row, property) == null) {
            set(row, property, value);
        }
    }

    private void setIfBlank(Object row, String property, String value) {
        Object current = get(row, property);
        if (current == null || StringUtils.isBlank(String.valueOf(current))) {
            set(row, property, value);
        }
    }

    private Object get(Object row, String property) {
        try {
            Method method = row.getClass().getMethod("get" + capitalize(property));
            return method.invoke(row);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object invoke(Object target, String methodName, Object argument) {
        if (argument == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterTypes().length != 1) {
                continue;
            }
            try {
                return method.invoke(target, convert(argument, method.getParameterTypes()[0]));
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return longValue(String.valueOf(value));
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(String.valueOf(value));
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Date.class && value instanceof Date) {
            return value;
        }
        return value;
    }

    private String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String decapitalize(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private String camelToSnake(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
