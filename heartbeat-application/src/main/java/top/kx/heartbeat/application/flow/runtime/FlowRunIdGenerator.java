package top.kx.heartbeat.application.flow.runtime;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class FlowRunIdGenerator {
    public String nextId() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE));
    }
}
