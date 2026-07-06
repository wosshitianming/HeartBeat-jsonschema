package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;

public interface PlatformNoticeRepository {

    List<DomainRecord> listNotices();
}
