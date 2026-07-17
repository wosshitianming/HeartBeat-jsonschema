package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class FlowableRuntimeConfiguration {
    private static final List<FlowableEngineEventType> EVENTS = Arrays.asList(
            FlowableEngineEventType.PROCESS_STARTED,
            FlowableEngineEventType.PROCESS_COMPLETED,
            FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT,
            FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT,
            FlowableEngineEventType.PROCESS_CANCELLED,
            FlowableEngineEventType.ACTIVITY_STARTED,
            FlowableEngineEventType.ACTIVITY_COMPLETED,
            FlowableEngineEventType.ACTIVITY_CANCELLED,
            FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING,
            FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED,
            FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING,
            FlowableEngineEventType.ACTIVITY_SIGNALED,
            FlowableEngineEventType.ACTIVITY_CONDITIONAL_WAITING,
            FlowableEngineEventType.ACTIVITY_CONDITIONAL_RECEIVED,
            FlowableEngineEventType.TASK_CREATED,
            FlowableEngineEventType.TASK_COMPLETED,
            FlowableEngineEventType.TIMER_SCHEDULED,
            FlowableEngineEventType.TIMER_FIRED,
            FlowableEngineEventType.SEQUENCEFLOW_TAKEN,
            FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER);

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> flowProjectionConfigurer(
            FlowableProjectionEventListener listener,
            FlowableJobFailureCommittedEventListener committedJobFailureListener,
            FlowableJobFailureEventListener jobFailureListener) {
        return configuration -> {
            Map<String, List<FlowableEventListener>> typed = configuration.getTypedEventListeners() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(configuration.getTypedEventListeners());
            for (FlowableEngineEventType type : EVENTS) {
                List<FlowableEventListener> listeners = new ArrayList<>(typed.getOrDefault(type.name(), Collections.emptyList()));
                listeners.add(listener);
                typed.put(type.name(), listeners);
            }
            List<FlowableEventListener> jobFailureListeners = new ArrayList<>(typed.getOrDefault(
                    FlowableEngineEventType.JOB_EXECUTION_FAILURE.name(), Collections.emptyList()));
            jobFailureListeners.add(committedJobFailureListener);
            jobFailureListeners.add(jobFailureListener);
            typed.put(FlowableEngineEventType.JOB_EXECUTION_FAILURE.name(), jobFailureListeners);
            configuration.setTypedEventListeners(typed);
        };
    }
}
