package top.kx.heartbeat.application.flow.request;

import lombok.Data;

import java.util.Map;

@Data
public class FlowRetryRequest {
    private String reason;
    private Map<String, Object> variables;
}
