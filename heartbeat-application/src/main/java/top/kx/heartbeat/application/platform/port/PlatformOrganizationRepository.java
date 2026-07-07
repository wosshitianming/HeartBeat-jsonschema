// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.platform.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.request.PlatformDepartmentRequest;

import java.util.List;

/**
 * 注释：当前接口用于声明对应业务能力。
 */
public interface PlatformOrganizationRepository {

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listDepartments();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord createDepartment(PlatformDepartmentRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord updateDepartment(String id, PlatformDepartmentRequest request);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    void deleteDepartment(String id);

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listTenants();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listPosts();
// 注释：结束当前代码块。
}
