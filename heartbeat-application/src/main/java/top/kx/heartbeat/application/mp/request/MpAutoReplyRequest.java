package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MpAutoReplyRequest {

    private String id;
    @JsonAlias("account_id")
    private String accountId;
    private String keyword;
    @JsonAlias("match_type")
    private String matchType;
    @JsonAlias("reply_type")
    private String replyType;
    private Integer sortNo;
    private String status;
    @JsonAlias("content")
    private Object replyContent;
}
