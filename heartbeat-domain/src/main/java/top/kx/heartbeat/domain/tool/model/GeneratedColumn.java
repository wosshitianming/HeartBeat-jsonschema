package top.kx.heartbeat.domain.tool.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GeneratedColumn {
    Long id;
    Long tenantId;
    Long tableId;
    String columnName;
    String columnComment;
    String dataType;
    String javaType;
    String javaField;
    boolean primaryKey;
    boolean autoIncrement;
    boolean nullable;
    int sortNo;
}
