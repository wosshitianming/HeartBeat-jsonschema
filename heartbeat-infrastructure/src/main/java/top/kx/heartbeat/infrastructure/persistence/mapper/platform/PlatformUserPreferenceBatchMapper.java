package top.kx.heartbeat.infrastructure.persistence.mapper.platform;

import org.apache.ibatis.annotations.Param;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserPreferenceDO;

import java.util.List;

public interface PlatformUserPreferenceBatchMapper {

    int upsert(@Param("rows") List<SysUserPreferenceDO> rows);
}
