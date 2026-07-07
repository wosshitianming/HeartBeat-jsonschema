// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.mp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mp.port.*;
import top.kx.heartbeat.application.mp.request.MpAccountRequest;
import top.kx.heartbeat.application.mp.request.MpAutoReplyRequest;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Service
public class MpService {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpAccountRepository accountRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpMenuRepository menuRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpMaterialRepository materialRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpAutoReplyRepository autoReplyRepository;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpMenuSyncGateway mpMenuSyncGateway;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listAccounts() {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(accountRepository.listAccounts()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse getAccount(String id) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(accountRepository.getAccount(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveAccount(MpAccountRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(accountRepository.saveAccount(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listMenus(String accountId) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(menuRepository.listMenus(accountId)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveMenu(MpMenuRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(menuRepository.saveMenu(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public RecordResponse syncMenu(String accountId) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> account = accountRepository.getAccount(accountId).toMap();
        // 注释：设置或计算当前变量值。
        List<Map<String, Object>> menus = maps(menuRepository.listMenus(accountId));
        // 注释：返回当前处理结果。
        return mpMenuSyncGateway.syncMenus(account, menus);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listMaterials(String accountId) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(materialRepository.listMaterials(accountId)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveMaterial(MpMaterialRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(materialRepository.saveMaterial(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    public List<RecordResponse> listAutoReplies(String accountId) {
        // 注释：返回当前处理结果。
        return RecordResponse.fromMaps(maps(autoReplyRepository.listAutoReplies(accountId)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Transactional
    public RecordResponse saveAutoReply(MpAutoReplyRequest request) {
        // 注释：返回当前处理结果。
        return RecordResponse.from(autoReplyRepository.saveAutoReply(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        // 注释：返回当前处理结果。
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
