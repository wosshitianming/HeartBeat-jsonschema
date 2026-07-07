// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Data
public class MpMenuRequest {

    // 注释：声明当前成员或方法。
    private String id;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("account_id")
    // 注释：声明当前成员或方法。
    private String accountId;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias("parent_id")
    // 注释：声明当前成员或方法。
    private String parentId;
    // 注释：声明当前成员或方法。
    private String name;
    // 注释：声明当前元素使用的注解配置。
    @JsonAlias({"menu_type", "type"})
    // 注释：声明当前成员或方法。
    private String menuType;
    // 注释：声明当前成员或方法。
    private String url;
    // 注释：声明当前成员或方法。
    private Integer sortNo;
    // 注释：声明当前成员或方法。
    private String status;
    // 注释：声明当前成员或方法。
    private Object payload;
// 注释：结束当前代码块。
}
