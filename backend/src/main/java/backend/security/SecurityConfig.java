package backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final CustomHeaderCsrfFilter customHeaderCsrfFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final SensitiveRequestProtectionFilter sensitiveRequestProtectionFilter;

    @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}")
    private String corsAllowedOrigins;

    public SecurityConfig(
            DatabaseUserDetailsService userDetailsService,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler,
            CustomHeaderCsrfFilter customHeaderCsrfFilter,
            SecurityHeadersFilter securityHeadersFilter,
            SensitiveRequestProtectionFilter sensitiveRequestProtectionFilter
    ) {
        this.userDetailsService = userDetailsService;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
        this.apiAccessDeniedHandler = apiAccessDeniedHandler;
        this.customHeaderCsrfFilter = customHeaderCsrfFilter;
        this.securityHeadersFilter = securityHeadersFilter;
        this.sensitiveRequestProtectionFilter = sensitiveRequestProtectionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // We deliberately disable Spring's stateful CSRF token in favour of a
                // custom-header check (CustomHeaderCsrfFilter) plus SameSite cookies
                // and a locked-down CORS allow-list. See README "External Threat
                // Protection" for the trade-off rationale.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fix -> fix.changeSessionId()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/ui/login", "/ui/register", "/auth/register", "/auth/login", "/css/**").permitAll()
                        .requestMatchers("/send", "/ui/send", "/message/send").hasRole("SENDER")
                        .requestMatchers("/receive", "/ui/decrypt/**", "/message/received", "/message/decrypt/**").hasRole("RECEIVER")
                        .requestMatchers("/puzzle/**").hasRole("RECEIVER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/attack/**").hasAnyRole("SENDER", "RECEIVER")
                        .requestMatchers("/simulation/**").hasAnyRole("SENDER", "RECEIVER")
                        .requestMatchers("/evaluation/**").hasAnyRole("SENDER", "RECEIVER", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler))
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(customHeaderCsrfFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sensitiveRequestProtectionFilter, CustomHeaderCsrfFilter.class)
                .authenticationProvider(authenticationProvider())
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("NSG_SESSION", "JSESSIONID"));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        // Never combine credentials with a wildcard origin — that's an explicit
        // OWASP misconfiguration. Each origin is matched exactly.
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With",
                "X-Admin-Confirm", "X-Req-Nonce", "X-Req-Ts", "X-Req-Sig", "Accept", "Accept-Language"));
        config.setExposedHeaders(List.of("Retry-After", "X-NSG-Throttle-Ms"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
