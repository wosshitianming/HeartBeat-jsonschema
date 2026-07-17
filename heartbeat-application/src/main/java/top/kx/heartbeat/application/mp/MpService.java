package top.kx.heartbeat.application.mp;

import org.apache.commons.lang3.StringUtils;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 编排公众号管理应用用例，承接接口层请求并协调仓储与领域能力。
 */
@Service
public class MpService {

    @Resource
    private MpAccountRepository accountRepository;

    @Resource
    private MpMenuRepository menuRepository;

    @Resource
    private MpMaterialRepository materialRepository;

    @Resource
    private MpAutoReplyRepository autoReplyRepository;

    @Resource
    private MpMenuSyncGateway mpMenuSyncGateway;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调公众号管理相关仓储和领域规则。
     *
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listAccounts() {
        return accountRepository.listAccounts().stream()
                .map(this::maskedAccount)
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，协调公众号管理相关仓储和领域规则。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse getAccount(String id) {
        return maskedAccount(accountRepository.getAccount(id));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调公众号管理相关仓储和领域规则。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveAccount(MpAccountRequest request) {
        return maskedAccount(accountRepository.saveAccount(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调公众号管理相关仓储和领域规则。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listMenus(String accountId) {
        return RecordResponse.fromMaps(maps(menuRepository.listMenus(accountId)));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调公众号管理相关仓储和领域规则。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveMenu(MpMenuRequest request) {
        return RecordResponse.from(menuRepository.saveMenu(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，协调公众号管理相关仓储和领域规则。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public RecordResponse syncMenu(String accountId) {
        Map<String, Object> account = accountRepository.getAccount(accountId).toMap();
        List<Map<String, Object>> menus = maps(menuRepository.listMenus(accountId));
        return mpMenuSyncGateway.syncMenus(account, menus);
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调公众号管理相关仓储和领域规则。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listMaterials(String accountId) {
        return RecordResponse.fromMaps(maps(materialRepository.listMaterials(accountId)));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调公众号管理相关仓储和领域规则。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveMaterial(MpMaterialRequest request) {
        return RecordResponse.from(materialRepository.saveMaterial(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，协调公众号管理相关仓储和领域规则。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    public List<RecordResponse> listAutoReplies(String accountId) {
        return RecordResponse.fromMaps(maps(autoReplyRepository.listAutoReplies(accountId)));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，协调公众号管理相关仓储和领域规则。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Transactional
    public RecordResponse saveAutoReply(MpAutoReplyRequest request) {
        return RecordResponse.from(autoReplyRepository.saveAutoReply(request));
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，协调公众号管理相关仓储和领域规则。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }

    private RecordResponse maskedAccount(DomainRecord record) {
        Map<String, Object> account = new LinkedHashMap<>(record.toMap());
        account.put("appSecret", redact(account.get("appSecret")));
        account.put("token", redact(account.get("token")));
        account.put("aesKey", redact(account.get("aesKey")));
        return RecordResponse.from(account);
    }

    private String redact(Object value) {
        return StringUtils.isBlank(value == null ? null : String.valueOf(value)) ? "" : "******";
    }
}
