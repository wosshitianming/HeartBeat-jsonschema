package top.kx.heartbeat.application.mp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载公众号管理请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
