package top.kx.heartbeat.infrastructure.config;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz 与 Spring 集成：支持 Job 内 @Resource 注入。
 *
 * <p>仅在 Quartz 中间件启用时生效，避免未启用时加载调度器相关 Bean。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "true")
public class QuartzConfig {

    /**
     * 自定义 JobFactory，使 Quartz Job 支持 Spring 依赖注入。
     *
     * @param applicationContext Spring 应用上下文。
     * @return 支持自动装配的 Quartz JobFactory。
     */
    @Bean
    public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
        // 创建支持 Spring 自动装配的 Quartz JobFactory。
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        // 注入 Spring 应用上下文。
        jobFactory.setApplicationContext(applicationContext);
        // 返回 Spring 托管的 Quartz JobFactory。
        return jobFactory;
    }

    /**
     * 将 Spring JobFactory 挂到 Boot 自动配置的 SchedulerFactoryBean 上。
     *
     * @param springBeanJobFactory 支持自动装配的 Quartz JobFactory。
     * @return SchedulerFactoryBean 定制器。
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(SpringBeanJobFactory springBeanJobFactory) {
        // 返回调度器工厂定制器，将 JobFactory 接入 Quartz。
        return factoryBean -> factoryBean.setJobFactory(springBeanJobFactory);
    }

    /**
     * 支持自动装配的 Quartz JobFactory。
     */
    static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

        /**
         * Spring 自动装配 Bean 工厂。
         */
        private AutowireCapableBeanFactory beanFactory;

        /**
         * 注入 Spring 应用上下文。
         *
         * @param applicationContext Spring 应用上下文。
         */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            // 保持父类对应用上下文的处理。
            super.setApplicationContext(applicationContext);
            // 获取 Spring 自动装配 Bean 工厂。
            this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        /**
         * 创建 Quartz Job 实例。
         *
         * @param bundle Quartz 触发上下文。
         * @return 自动装配后的 Job 实例。
         * @throws Exception Job 创建异常。
         */
        @Override
        protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
            // 使用父类创建原始 Job 实例。
            Object job = super.createJobInstance(bundle);
            // 对 Job 实例执行 Spring 依赖注入。
            beanFactory.autowireBean(job);
            // 返回完成自动装配的 Job 实例。
            return job;
        }
    }
}
