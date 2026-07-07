// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformUserRequest;

import java.util.List;
import java.util.Optional;

/**
 * 注释：当前接口用于声明对应业务能力。
 */
public interface PlatformUserRepository {

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    Optional<DomainRecord> findUserByUsername(String username);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    Optional<DomainRecord> findUserById(String userId);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    Optional<DomainRecord> findUserPreference(String userId, String preferenceKey);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord saveUserPreference(String userId, String preferenceKey, String preferenceValue);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listUsers();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord createUser(PlatformUserRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord updateUser(String id, PlatformUserRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    void deleteUser(String id);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord createSocialUser(PlatformUserRequest request);
// 注释：结束当前代码块。
}
