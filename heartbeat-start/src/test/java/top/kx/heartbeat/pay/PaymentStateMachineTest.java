package top.kx.heartbeat.pay;

import org.junit.jupiter.api.Test;
import top.kx.heartbeat.infrastructure.pay.PayFlexRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayOrderEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentStateMachineTest {

    private final PayFlexRepository repository = new PayFlexRepository();

    @Test
    void allowsOnlyConfiguredPaymentTransitions() {
        PayOrderEntity order = new PayOrderEntity();
        order.setStatus("CREATED");

        repository.transition(order, "PAYING");
        assertEquals("PAYING", order.getStatus());

        repository.transition(order, "PAID");
        assertEquals("PAID", order.getStatus());

        repository.transition(order, "PART_REFUNDED");
        assertEquals("PART_REFUNDED", order.getStatus());

        repository.transition(order, "REFUNDED");
        assertEquals("REFUNDED", order.getStatus());
    }

    @Test
    void rejectsSkippedOrBackwardPaymentTransitions() {
        PayOrderEntity order = new PayOrderEntity();
        order.setStatus("CREATED");

        assertThrows(IllegalStateException.class, () -> repository.transition(order, "PAID"));
    }
}
