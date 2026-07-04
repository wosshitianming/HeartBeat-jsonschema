package top.kx.heartbeat.domain.tool.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GeneratedTable {
    Long id;
    Long tenantId;
    String tableName;
    String tableComment;
    String className;
    String moduleName;
    String basePackage;
    String resourceKey;
    String optionsJson;
    String status;
}
