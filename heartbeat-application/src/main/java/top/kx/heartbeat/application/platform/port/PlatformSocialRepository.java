// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformSocialBindRequest;
import top.kx.heartbeat.application.platform.request.PlatformSocialProviderRequest;

import java.util.List;
import java.util.Optional;

/**
 * 注释：当前接口用于声明对应业务能力。
 */
public interface PlatformSocialRepository {

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listSocialProviders();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord createSocialProvider(PlatformSocialProviderRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord updateSocialProvider(String id, PlatformSocialProviderRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    void deleteSocialProvider(String id);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listActiveSocialProviders();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    Optional<DomainRecord> findSocialProvider(String provider);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    Optional<DomainRecord> findSocialBind(String provider, String openId);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord saveSocialBind(PlatformSocialBindRequest request);
// 注释：结束当前代码块。
}
