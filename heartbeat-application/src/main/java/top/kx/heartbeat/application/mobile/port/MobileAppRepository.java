// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.mobile.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;

import java.util.List;

/**
 * 注释：当前接口用于声明对应业务能力。
 */
public interface MobileAppRepository {

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    List<DomainRecord> listApps();

    // 注释：执行当前代码行。

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    DomainRecord saveApp(MobileAppRequest request);
// 注释：结束当前代码块。
}
