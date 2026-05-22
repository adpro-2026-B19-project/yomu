package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.auth.service.OAuth2LoginUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            ObjectProvider<OAuth2LoginUserService> oauth2LoginUserServiceProvider,
            LoginRateLimitFilter loginRateLimitFilter,
            RateLimitedAuthenticationFailureHandler authenticationFailureHandler,
            RateLimitedAuthenticationSuccessHandler authenticationSuccessHandler
    ) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/auth/**",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus").hasRole("ADMIN")
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").authenticated()
                .requestMatchers("/profile/**").authenticated()
                .anyRequest().authenticated()
        );

        http.formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("identifier")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
        );

        OAuth2LoginUserService oauth2LoginUserService = oauth2LoginUserServiceProvider.getIfAvailable();
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null && oauth2LoginUserService != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(oauth2LoginUserService))
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/auth/login?error")
            );
        }

        http.logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        if (h2ConsoleEnabled) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self' data:; " +
                        "object-src 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self' https://accounts.google.com"
                ))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
        );

        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
