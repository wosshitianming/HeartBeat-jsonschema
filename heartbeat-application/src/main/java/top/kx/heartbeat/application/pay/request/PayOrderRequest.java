// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.pay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class PayOrderRequest {

    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("order_no")
    // 注释：声明当前成员或方法。
    private String orderNo;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("channel_id")
    // 注释：声明当前成员或方法。
    private String channelId;
    // 注释：声明当前成员或方法。
    private String subject;
    // 注释：声明当前成员或方法。
    private BigDecimal amount;
    // 注释：声明当前成员或方法。
    private String currency;
    // 注释：声明当前成员或方法。
    private String status;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("client_ip")
    // 注释：声明当前成员或方法。
    private String clientIp;
    // 注释：声明当前成员或方法。
    private Object extra;
// 注释：结束当前代码块。
}
