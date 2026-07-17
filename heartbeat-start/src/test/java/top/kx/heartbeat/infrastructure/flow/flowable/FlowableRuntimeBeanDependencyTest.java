package top.kx.heartbeat.infrastructure.flow.flowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class FlowableRuntimeBeanDependencyTest {

    @Test
    void runtimeAndExternalIoDispatcherCanBeCreatedWithoutCircularReferences() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            assertNotNull(context.getBean(FlowableRuntimeService.class));
            assertNotNull(context.getBean(FlowExternalIoCommandDispatcher.class));
            assertNotNull(context.getBean(FlowExternalIoCommandCancellationService.class));
        }
    }

    @Configuration
    @ComponentScan(
            basePackageClasses = FlowableRuntimeService.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                            FlowableRuntimeService.class,
                            FlowExternalIoCommandDispatcher.class,
                            FlowExternalIoCommandCancellationService.class
                    }))
    static class TestConfiguration {

        @Bean
        RuntimeService nativeFlowableRuntimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        RepositoryService nativeFlowableRepositoryService() {
            return mock(RepositoryService.class);
        }

        @Bean
        FlowableVariableCodec flowableVariableCodec() {
            return mock(FlowableVariableCodec.class);
        }

        @Bean
        FlowablePayloadStore flowablePayloadStore() {
            return mock(FlowablePayloadStore.class);
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        SecretCryptoService secretCryptoService() {
            return mock(SecretCryptoService.class);
        }
    }
}
