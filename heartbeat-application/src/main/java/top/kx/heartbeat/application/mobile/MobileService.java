package top.kx.heartbeat.application.mobile;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobileRepository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 移动端配置应用服务。
 *
 * <p>负责移动应用、页面和 API 路由的应用层编排。</p>
 */
@Service
public class MobileService {

    /**
     * 移动端配置仓储。
     */
    @Resource
    private MobileRepository mobileRepository;

    /**
     * 查询移动应用列表。
     *
     * @return 移动应用列表。
     */
    public List<Map<String, Object>> listApps() {
        // 查询移动应用领域记录并转换为字段 Map 列表。
        return maps(mobileRepository.listApps());
    }

    /**
     * 保存移动应用。
     *
     * @param command 移动应用保存命令。
     * @return 保存后的移动应用。
     */
    @Transactional
    public Map<String, Object> saveApp(Map<String, Object> command) {
        // 委托仓储保存移动应用并返回字段 Map。
        return mobileRepository.saveApp(command).toMap();
    }

    /**
     * 查询移动应用页面列表。
     *
     * @param appId 移动应用标识。
     * @return 移动应用页面列表。
     */
    public List<Map<String, Object>> listPages(String appId) {
        // 查询移动应用页面领域记录并转换为字段 Map 列表。
        return maps(mobileRepository.listPages(appId));
    }

    /**
     * 保存移动应用页面。
     *
     * @param command 移动应用页面保存命令。
     * @return 保存后的移动应用页面。
     */
    @Transactional
    public Map<String, Object> savePage(Map<String, Object> command) {
        // 委托仓储保存移动应用页面并返回字段 Map。
        return mobileRepository.savePage(command).toMap();
    }

    /**
     * 查询移动应用 API 路由列表。
     *
     * @param appId 移动应用标识。
     * @return 移动应用 API 路由列表。
     */
    public List<Map<String, Object>> listApiRoutes(String appId) {
        // 查询移动应用 API 路由领域记录并转换为字段 Map 列表。
        return maps(mobileRepository.listApiRoutes(appId));
    }

    /**
     * 保存移动应用 API 路由。
     *
     * @param command 移动应用 API 路由保存命令。
     * @return 保存后的移动应用 API 路由。
     */
    @Transactional
    public Map<String, Object> saveApiRoute(Map<String, Object> command) {
        // 委托仓储保存移动应用 API 路由并返回字段 Map。
        return mobileRepository.saveApiRoute(command).toMap();
    }

    /**
     * 将领域记录列表转换为字段 Map 列表。
     *
     * @param records 领域记录列表。
     * @return 字段 Map 列表。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 逐条导出领域记录的字段副本。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
