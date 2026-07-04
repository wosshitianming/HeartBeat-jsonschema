package top.kx.heartbeat.infrastructure.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.kx.heartbeat.infrastructure.tenant.TenantContextFilter;

import javax.annotation.Resource;

/**
 * Spring Security 安全配置。
 *
 * <p>无状态 JWT 模式：挂载 {@link JwtAuthenticationFilter}，登录与社交回调路径放行。</p>
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // JWT 认证过滤器
    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    // 多租户上下文过滤器
    @Resource
    private TenantContextFilter tenantContextFilter;

    /**
     * 配置 HTTP 安全过滤链。
     *
     * @param http HttpSecurity 构建器
     * @return 已组装的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 关闭 CSRF（前后端分离 + JWT）
        http.csrf().disable()
                // 无状态会话
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 路径授权规则
                .authorizeRequests()
                // 预检请求放行
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 登录、刷新、社交回调、H2 控制台放行
                .antMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/social/**",
                        "/h2-console/**",
                        "/error"
                ).permitAll()
                // 过渡期：业务 API 仍放行，由 Controller 自行解析用户
                .antMatchers("/api/v1/**").authenticated()
                // 其余请求放行（静态资源等）
                .anyRequest().permitAll()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                // H2 控制台允许 iframe
                .headers().frameOptions().sameOrigin()
                .and()
                // JWT 过滤器置于用户名密码过滤器之前（须先注册，供下方租户过滤器定位）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 租户上下文过滤器置于 JWT 过滤器之前
                .addFilterBefore(tenantContextFilter, JwtAuthenticationFilter.class);
        // 构建过滤链
        return http.build();
    }
}
