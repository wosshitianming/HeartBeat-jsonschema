package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载公众号管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class MpMaterialRequest {

    private String id;
    @JsonAlias("account_id")
    private String accountId;
    @JsonAlias({"material_type", "type"})
    private String materialType;
    private String title;
    @JsonAlias("media_id")
    private String mediaId;
    private String url;
    private String status;
    private Object payload;
}
