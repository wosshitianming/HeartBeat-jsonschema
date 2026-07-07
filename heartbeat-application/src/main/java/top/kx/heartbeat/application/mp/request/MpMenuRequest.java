package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载公众号管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class MpMenuRequest {

    private String id;
    @JsonAlias("account_id")
    private String accountId;
    @JsonAlias("parent_id")
    private String parentId;
    private String name;
    @JsonAlias({"menu_type", "type"})
    private String menuType;
    private String url;
    private Integer sortNo;
    private String status;
    private Object payload;
}
