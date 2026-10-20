package com.logistics.system.security;

import com.logistics.system.security.filter.JwtAuthFilter;
import com.logistics.system.security.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/customers/**", "/api/orders/**").hasAnyRole("ADMIN", "OPERATIONS")
                .requestMatchers(HttpMethod.POST, "/api/customers/**", "/api/orders/**").hasAnyRole("ADMIN", "OPERATIONS")
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole("ADMIN", "OPERATIONS")
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/warehouses/**", "/api/inventory/**").hasAnyRole("ADMIN", "OPERATIONS", "WAREHOUSE_USER")
                .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/warehouses/**", "/api/inventory/**").hasAnyRole("ADMIN", "OPERATIONS", "WAREHOUSE_USER")
                .requestMatchers(HttpMethod.GET, "/api/carriers/**", "/api/shipments/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.POST, "/api/carriers/**", "/api/shipments/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.PUT, "/api/shipments/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.GET, "/api/drivers/**", "/api/vehicles/**", "/api/deliveries/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.POST, "/api/drivers/**", "/api/vehicles/**", "/api/deliveries/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.PUT, "/api/deliveries/**").hasAnyRole("ADMIN", "OPERATIONS", "DISPATCHER")
                .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "OPERATIONS")
                .requestMatchers("/api/admin/modules/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/me/modules").authenticated()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
