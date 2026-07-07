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

@Service
public class MobileService {

    @Resource
    private MobileAppRepository mobileAppRepository;
    @Resource
    private MobilePageRepository mobilePageRepository;
    @Resource
    private MobileApiRouteRepository mobileApiRouteRepository;

    public List<RecordResponse> listApps() {
        return RecordResponse.fromMaps(maps(mobileAppRepository.listApps()));
    }

    @Transactional
    public RecordResponse saveApp(MobileAppRequest request) {
        return RecordResponse.from(mobileAppRepository.saveApp(request));
    }

    public List<RecordResponse> listPages(String appId) {
        return RecordResponse.fromMaps(maps(mobilePageRepository.listPages(appId)));
    }

    @Transactional
    public RecordResponse savePage(MobilePageRequest request) {
        return RecordResponse.from(mobilePageRepository.savePage(request));
    }

    public List<RecordResponse> listApiRoutes(String appId) {
        return RecordResponse.fromMaps(maps(mobileApiRouteRepository.listApiRoutes(appId)));
    }

    @Transactional
    public RecordResponse saveApiRoute(MobileApiRouteRequest request) {
        return RecordResponse.from(mobileApiRouteRepository.saveApiRoute(request));
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(java.util.stream.Collectors.toList());
    }
}
