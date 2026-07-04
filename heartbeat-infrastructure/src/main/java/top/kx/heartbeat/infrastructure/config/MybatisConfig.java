package top.kx.heartbeat.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 全局配置：Mapper 扫描 + 多租户拦截由后续自定义拦截器实现。
 * <p>
 * 扫描范围包含手写 Mapper（{@code persistence.mapper}）与 MyBatis Generator 生成 Mapper
 * （{@code persistence.mapper.gen}），互不干扰。
 * </p>
 *
 * @author heartbeat-team
 */
@Configuration
@MapperScan({
        "top.kx.heartbeat.infrastructure.persistence.mapper",
        "top.kx.heartbeat.infrastructure.persistence.mapper.gen"
})
public class MybatisConfig {
}