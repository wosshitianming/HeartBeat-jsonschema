package top.kx.heartbeat.application.flow.runtime;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlled resolution for an ambiguous external side effect.
 */
@Data
public class FlowExternalIoResolutionRequest {

    private String outcomeStatus;

    private boolean confirmedNotExecuted;

    private Map<String, Object> output = new LinkedHashMap<>();

    private String errorCode;

    private String errorMessage;
}
