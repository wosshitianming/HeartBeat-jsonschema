package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.user.UserApplicationService;
import top.kx.heartbeat.application.user.command.ChangeEmailCommand;
import top.kx.heartbeat.application.user.command.RegisterUserCommand;
import top.kx.heartbeat.application.user.dto.UserDTO;
import top.kx.heartbeat.support.MySqlIntegrationTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 持久化栈契约测试（已迁移到 MyBatis + MBG 后继续验证 ORM 单源策略）
 */
@SpringBootTest
@ActiveProfiles("local")
class MybatisOnlyPersistenceTest extends MySqlIntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserApplicationService userApplicationService;

    @Test
    void doesNotCreateJpaInfrastructure() {
        assertFalse(applicationContext.containsBean("entityManagerFactory"));
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("org.springframework.data.jpa.repository.JpaRepository")
        );
    }

    @Test
    void doesNotKeepDynamicBusinessSqlMapperInProductionSource() {
        Path root = projectRoot();
        assertFalse(Files.exists(root.resolve("heartbeat-infrastructure/src/main/java/top/kx/heartbeat/"
                        + "infrastructure/persistence/mapper/BusinessSqlMapper.java")),
                "Business CRUD must use focused MyBatis mappers (MBG generated) instead of dynamic SQL");
        assertFalse(Files.exists(root.resolve("heartbeat-infrastructure/src/main/java/top/kx/heartbeat/"
                        + "infrastructure/business/BusinessFlexRepository.java")),
                "Business domains must not share the legacy Map-based repository");
    }

    @Test
    void persistsUsersThroughTheDomainRepository() {
        String suffix = String.valueOf(System.nanoTime());
        String originalEmail = "mybatis-" + suffix + "@example.com";
        UserDTO saved = userApplicationService.register(
                new RegisterUserCommand("mybatis-user-" + suffix, originalEmail));

        assertTrue(saved.getId() > 0);
        assertEquals(originalEmail, saved.getEmail());
        assertEquals(saved.getId(), userApplicationService.getById(saved.getId()).getId());

        String changedEmail = "mybatis-updated-" + suffix + "@example.com";
        UserDTO updated = userApplicationService.changeEmail(
                new ChangeEmailCommand(saved.getId(), changedEmail));
        assertEquals(changedEmail, updated.getEmail());
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        if (Files.exists(current.resolve("heartbeat-start"))) {
            return current;
        }
        Path parent = current.getParent();
        while (parent != null) {
            if (Files.exists(parent.resolve("heartbeat-start"))) {
                return parent;
            }
            parent = parent.getParent();
        }
        return current;
    }
}
