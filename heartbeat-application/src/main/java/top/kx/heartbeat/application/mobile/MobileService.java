// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class MobileService {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileAppRepository mobileAppRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobilePageRepository mobilePageRepository;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileApiRouteRepository mobileApiRouteRepository;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listApps() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(mobileAppRepository.listApps()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveApp(MobileAppRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(mobileAppRepository.saveApp(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listPages(String appId) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(mobilePageRepository.listPages(appId)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse savePage(MobilePageRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(mobilePageRepository.savePage(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listApiRoutes(String appId) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(mobileApiRouteRepository.listApiRoutes(appId)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveApiRoute(MobileApiRouteRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(mobileApiRouteRepository.saveApiRoute(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
