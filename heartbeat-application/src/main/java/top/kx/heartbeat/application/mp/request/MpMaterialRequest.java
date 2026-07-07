package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

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
