package top.kx.heartbeat.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;
import top.kx.heartbeat.domain.user.service.UserRegistrationService;

/**
 * 领域服务由 Spring 容器创建，服务内部依赖统一使用 {@code @Resource} 注入。
 *
 * <p>领域模块保持纯净不依赖 Spring，应用层通过配置类把无状态领域服务注册为 Spring Bean。</p>
 */
@Configuration
public class DomainServiceConfig {

    /**
     * 注册用户注册领域服务。
     *
     * @return 用户注册领域服务
     */
    @Bean
    public UserRegistrationService userRegistrationService() {
        // 创建用户注册领域服务实例。
        return new UserRegistrationService();
    }

    /**
     * 注册流程 DSL 校验领域服务。
     *
     * @return 流程 DSL 校验领域服务
     */
    @Bean
    public FlowDslValidator flowDslValidator() {
        // 创建流程 DSL 校验领域服务实例。
        return new FlowDslValidator();
    }
}
