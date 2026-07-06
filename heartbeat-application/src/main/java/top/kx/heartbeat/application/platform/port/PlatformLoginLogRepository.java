package top.kx.heartbeat.application.platform.port;

/**
 * 平台登录日志仓储。
 */
public interface PlatformLoginLogRepository {

    void recordLogin(String username, String status, String message);
}
