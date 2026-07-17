package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import top.kx.heartbeat.support.MySqlIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 事务管理器装配测试（MyBatis 已迁移到 MyBatis Generator 后继续验证 Spring 事务边界）
 */
@SpringBootTest
@ActiveProfiles("local")
class TransactionManagerConfigurationTest extends MySqlIntegrationTestSupport {

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 验证 Spring 仍以 DataSourceTransactionManager 处理事务（兼容 MyBatis Generator）。
     */
    @Test
    void usesDataSourceTransactionManagerForGeneratedMapperPersistence() {
        assertInstanceOf(DataSourceTransactionManager.class, transactionManager);
    }
}
