package top.kx.heartbeat.infrastructure.flow.flowable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Cancels durable external-I/O commands that belong to a Flowable process run.
 *
 * <p>The cancellation operation is deliberately isolated from the dispatcher. Flowable runtime
 * cancellation needs this database update, while the dispatcher needs the runtime service to
 * resume completed commands. Keeping this narrow component between them prevents a circular
 * Spring bean dependency.</p>
 */
@Service
public class FlowExternalIoCommandCancellationService {

    private static final String CANCELED = "CANCELED";

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * Marks every non-terminal external-I/O command of the specified run as canceled.
     *
     * @param tenantId         trusted tenant identifier stored in the Flowable execution
     * @param runId            trusted run identifier stored in the Flowable execution
     * @param engineInstanceId Flowable process instance identifier
     * @param reason           cancellation reason
     * @return number of commands moved to the canceled state
     */
    @Transactional
    public int cancelByRun(String tenantId, String runId, String engineInstanceId, String reason) {
        long tenant = positiveLong(tenantId, "tenantId");
        long run = positiveLong(runId, "runId");
        Instant now = Instant.now();
        return jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, completed_at = ?, "
                        + "next_attempt_at = NULL, lease_owner = NULL, lease_until = NULL, lease_token = NULL, "
                        + "error_code = ?, error_message = ?, update_time = ? WHERE tenant_id = ? AND run_id = ? "
                        + "AND engine_instance_id = ? AND status NOT IN ('SUCCEEDED','FAILED_FINAL','TIMEOUT','CANCELED')",
                CANCELED, Timestamp.from(now), "FLOW_CANCELED", truncate(reason, 2000), Timestamp.from(now),
                tenant, run, engineInstanceId);
    }

    private long positiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(StringUtils.trimToEmpty(value));
            if (parsed > 0) return parsed;
        } catch (NumberFormatException ignored) {
            // Report a stable field-specific error below.
        }
        throw new IllegalStateException("缺少有效的 " + field);
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
