package top.kx.heartbeat.infrastructure.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.auth.AuthSession;
import top.kx.heartbeat.infrastructure.persistence.entity.platform.AuthSessionEntity;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuthSessionStructMapper {

    AuthSession toDomain(AuthSessionEntity entity);

    AuthSessionEntity toEntity(AuthSession session);
}
