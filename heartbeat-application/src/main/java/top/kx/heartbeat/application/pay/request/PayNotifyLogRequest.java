// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.pay.request;

import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PayNotifyLogRequest {

    // 注释：声明当前成员或方法。
    private String orderId;
    // 注释：声明当前成员或方法。
    private String orderNo;
    // 注释：声明当前成员或方法。
    private String provider;
    // 注释：声明当前成员或方法。
    private String payload;
    // 注释：声明当前成员或方法。
    private String status;
    // 注释：声明当前成员或方法。
    private String signatureValid;
// 注释：结束当前代码块。
}
