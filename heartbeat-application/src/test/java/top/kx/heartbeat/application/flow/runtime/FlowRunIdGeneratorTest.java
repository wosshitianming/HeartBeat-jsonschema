package top.kx.heartbeat.application.flow.runtime;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRunIdGeneratorTest {
    @Test
    void generatedIdsStayPositiveAndUniqueAcrossABurst() {
        FlowRunIdGenerator generator = new FlowRunIdGenerator();
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 10000; i++) {
            String id = generator.nextId();
            assertTrue(Long.parseLong(id) > 0L);
            ids.add(id);
        }

        assertEquals(10000, ids.size());
    }
}
