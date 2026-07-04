package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 公众号（WeChat MP）用用网关接口
 *
 * @author heartbeat-team
 */
public interface MpRepository {

    /**
     * 列出公众号账号
     */
    List<DomainRecord> listAccounts();

    /**
     * 查询单个公众号
     */
    DomainRecord getAccount(String id);

    /**
     * 保存/更新公众号
     */
    DomainRecord saveAccount(Map<String, Object> command);

    /**
     * 列出公众号的自定义菜单
     */
    List<DomainRecord> listMenus(String accountId);

    /**
     * 保存菜单
     */
    DomainRecord saveMenu(Map<String, Object> command);

    /**
     * 列出素材
     */
    List<DomainRecord> listMaterials(String accountId);

    /**
     * 保存素材
     */
    DomainRecord saveMaterial(Map<String, Object> command);

    /**
     * 列出自动回复规则
     */
    List<DomainRecord> listAutoReplies(String accountId);

    /**
     * 保存自动回复
     */
    DomainRecord saveAutoReply(Map<String, Object> command);
}
