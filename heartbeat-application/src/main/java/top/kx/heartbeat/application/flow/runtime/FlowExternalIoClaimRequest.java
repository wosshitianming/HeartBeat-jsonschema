package top.kx.heartbeat.application.flow.runtime;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker claim parameters for an external Flow I/O command.
 */
@Data
public class FlowExternalIoClaimRequest {

    private String workerId;

    private List<String> workerTopics = new ArrayList<>();

    private int leaseSeconds = 60;
}
