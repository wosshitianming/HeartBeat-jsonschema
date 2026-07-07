package top.kx.heartbeat.application.mobile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mobile.port.MobileApiRouteRepository;
import top.kx.heartbeat.application.mobile.port.MobileAppRepository;
import top.kx.heartbeat.application.mobile.port.MobilePageRepository;
import top.kx.heartbeat.application.mobile.request.MobileApiRouteRequest;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;
import top.kx.heartbeat.application.mobile.request.MobilePageRequest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 编排移动端配置应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class MobileService {

    @Resource
    private MobileAppRepository mobileAppRepository;
    @Resource
    private MobilePageRepository mobilePageRepository;
    @Resource
    private MobileApiRouteRepository mobileApiRouteRepository;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调移动端配置相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listApps() {
        return RecordResponse.fromMaps(maps(mobileAppRepository.listApps()));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调移动端配置相关仓储和领域规则。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveApp(MobileAppRequest request) {
        return RecordResponse.from(mobileAppRepository.saveApp(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调移动端配置相关仓储和领域规则。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listPages(String appId) {
        return RecordResponse.fromMaps(maps(mobilePageRepository.listPages(appId)));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调移动端配置相关仓储和领域规则。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse savePage(MobilePageRequest request) {
        return RecordResponse.from(mobilePageRepository.savePage(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调移动端配置相关仓储和领域规则。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listApiRoutes(String appId) {
        return RecordResponse.fromMaps(maps(mobileApiRouteRepository.listApiRoutes(appId)));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调移动端配置相关仓储和领域规则。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveApiRoute(MobileApiRouteRequest request) {
        return RecordResponse.from(mobileApiRouteRepository.saveApiRoute(request));
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调移动端配置相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
