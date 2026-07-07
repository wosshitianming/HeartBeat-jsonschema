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

    public List<RecordResponse> listAccounts() {
        return RecordResponse.fromMaps(maps(accountRepository.listAccounts()));
    }

    public RecordResponse getAccount(String id) {
        return RecordResponse.from(accountRepository.getAccount(id));
    }

    @Transactional
    public RecordResponse saveAccount(MpAccountRequest request) {
        return RecordResponse.from(accountRepository.saveAccount(request));
    }

    public List<RecordResponse> listMenus(String accountId) {
        return RecordResponse.fromMaps(maps(menuRepository.listMenus(accountId)));
    }

    @Transactional
    public RecordResponse saveMenu(MpMenuRequest request) {
        return RecordResponse.from(menuRepository.saveMenu(request));
    }

    public RecordResponse syncMenu(String accountId) {
        Map<String, Object> account = accountRepository.getAccount(accountId).toMap();
        List<Map<String, Object>> menus = maps(menuRepository.listMenus(accountId));
        return mpMenuSyncGateway.syncMenus(account, menus);
    }

    public List<RecordResponse> listMaterials(String accountId) {
        return RecordResponse.fromMaps(maps(materialRepository.listMaterials(accountId)));
    }

    @Transactional
    public RecordResponse saveMaterial(MpMaterialRequest request) {
        return RecordResponse.from(materialRepository.saveMaterial(request));
    }

    public List<RecordResponse> listAutoReplies(String accountId) {
        return RecordResponse.fromMaps(maps(autoReplyRepository.listAutoReplies(accountId)));
    }

    @Transactional
    public RecordResponse saveAutoReply(MpAutoReplyRequest request) {
        return RecordResponse.from(autoReplyRepository.saveAutoReply(request));
    }

    private List<Map<String, Object>> maps(List<DomainRecord> records) {
        return records.stream().map(DomainRecord::toMap).collect(Collectors.toList());
    }
}
