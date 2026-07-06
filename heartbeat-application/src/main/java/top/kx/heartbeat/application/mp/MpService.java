package top.kx.heartbeat.application.mp;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mp.port.MpMenuSyncGateway;
import top.kx.heartbeat.application.mp.port.MpRepository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 公众号应用服务。
 *
 * <p>负责编排公众号账号、菜单、素材、自动回复以及菜单同步能力。</p>
 */
@Service
public class MpService {

    /**
     * 公众号仓储。
     */
    @Resource
    private MpRepository mpRepository;

    /**
     * 公众号菜单同步网关。
     */
    @Resource
    private MpMenuSyncGateway mpMenuSyncGateway;

    /**
     * 查询公众号账号列表。
     *
     * @return 公众号账号列表。
     */
    public List<RecordResponse> listAccounts() {
        // 查询公众号账号领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(mpRepository.listAccounts()));
    }

    /**
     * 查询公众号账号详情。
     *
     * @param id 公众号账号标识。
     * @return 公众号账号详情。
     */
    public RecordResponse getAccount(String id) {
        // 委托仓储查询公众号账号并返回字段 Map。
        return RecordResponse.from(mpRepository.getAccount(id));
    }

    /**
     * 保存公众号账号。
     *
     * @param command 公众号账号保存命令。
     * @return 保存后的公众号账号。
     */
    @Transactional
    public RecordResponse saveAccount(Map<String, Object> command) {
        // 委托仓储保存公众号账号并返回字段 Map。
        return RecordResponse.from(mpRepository.saveAccount(command));
    }

    /**
     * 查询公众号菜单列表。
     *
     * @param accountId 公众号账号标识。
     * @return 公众号菜单列表。
     */
    public List<RecordResponse> listMenus(String accountId) {
        // 查询公众号菜单领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(mpRepository.listMenus(accountId)));
    }

    /**
     * 保存公众号菜单。
     *
     * @param command 公众号菜单保存命令。
     * @return 保存后的公众号菜单。
     */
    @Transactional
    public RecordResponse saveMenu(Map<String, Object> command) {
        // 委托仓储保存公众号菜单并返回字段 Map。
        return RecordResponse.from(mpRepository.saveMenu(command));
    }

    /**
     * 同步公众号菜单。
     *
     * @param accountId 公众号账号标识。
     * @return 菜单同步结果。
     */
    public RecordResponse syncMenu(String accountId) {
        // 查询公众号账号上下文。
        Map<String, Object> account = mpRepository.getAccount(accountId).toMap();
        // 查询公众号菜单上下文。
        List<Map<String, Object>> menus = maps(mpRepository.listMenus(accountId));
        // 委托菜单同步网关执行外部同步。
        return mpMenuSyncGateway.syncMenus(account, menus);
    }

    /**
     * 查询公众号素材列表。
     *
     * @param accountId 公众号账号标识。
     * @return 公众号素材列表。
     */
    public List<RecordResponse> listMaterials(String accountId) {
        // 查询公众号素材领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(mpRepository.listMaterials(accountId)));
    }

    /**
     * 保存公众号素材。
     *
     * @param command 公众号素材保存命令。
     * @return 保存后的公众号素材。
     */
    @Transactional
    public RecordResponse saveMaterial(Map<String, Object> command) {
        // 委托仓储保存公众号素材并返回字段 Map。
        return RecordResponse.from(mpRepository.saveMaterial(command));
    }

    /**
     * 查询公众号自动回复列表。
     *
     * @param accountId 公众号账号标识。
     * @return 公众号自动回复列表。
     */
    public List<RecordResponse> listAutoReplies(String accountId) {
        // 查询公众号自动回复领域记录并转换为字段 Map 列表。
        return RecordResponse.fromMaps(maps(mpRepository.listAutoReplies(accountId)));
    }

    /**
     * 保存公众号自动回复。
     *
     * @param command 公众号自动回复保存命令。
     * @return 保存后的公众号自动回复。
     */
    @Transactional
    public RecordResponse saveAutoReply(Map<String, Object> command) {
        // 委托仓储保存公众号自动回复并返回字段 Map。
        return RecordResponse.from(mpRepository.saveAutoReply(command));
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
