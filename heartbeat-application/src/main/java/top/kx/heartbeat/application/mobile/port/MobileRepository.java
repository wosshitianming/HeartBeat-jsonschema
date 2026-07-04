package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 移动端低代码用用网关接口
 *
 * @author heartbeat-team
 */
public interface MobileRepository {

    /**
     * 列出全部用用
     */
    List<DomainRecord> listApps();

    /**
     * 保存/更新用用
     */
    DomainRecord saveApp(Map<String, Object> command);

    /**
     * 列出用用下的页面
     */
    List<DomainRecord> listPages(String appId);

    /**
     * 保存页面
     */
    DomainRecord savePage(Map<String, Object> command);

    /**
     * 列出用用下的 API 路由
     */
    List<DomainRecord> listApiRoutes(String appId);

    /**
     * 保存 API 路由
     */
    DomainRecord saveApiRoute(Map<String, Object> command);
}
