package top.kx.heartbeat.infrastructure.tool.convert;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.tool.model.GeneratedTable;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;
import top.kx.heartbeat.infrastructure.persistence.entity.tool.SysGenTableEntity;
import top.kx.heartbeat.infrastructure.persistence.entity.tool.SysJobEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ToolConvert {

    GeneratedTable toDomain(SysGenTableEntity entity);

    ScheduledJob toDomain(SysJobEntity entity);
}
