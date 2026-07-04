package top.kx.heartbeat.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * MyBatis Generator 为主的单数据源事务边界。
 *
 * <p>统一注册主事务管理器，供应用服务的 {@code @Transactional} 使用。</p>
 */
@Configuration
public class PersistenceTransactionConfig {

    /**
     * 注册主事务管理器。
     *
     * @param dataSource 主数据源。
     * @return 主事务管理器。
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        // 使用 Spring 托管的数据源创建 JDBC 事务管理器。
        return new DataSourceTransactionManager(dataSource);
    }
}
