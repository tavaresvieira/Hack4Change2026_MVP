package org.resourcebridge.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                // =============================
                // Allow Svelte frontend static files
                // =============================
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/robots.txt",
                        "/favicon.ico",
                        "/_app/**"
                ).permitAll()

                // =============================
                // Public API endpoints
                // =============================
                .requestMatchers("/api/auth/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/needs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organizations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()

                // =============================
                // Shelter Staff endpoints
                // =============================
                .requestMatchers(HttpMethod.POST, "/api/needs").hasRole("STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/needs/*/fulfill").hasRole("STAFF")

                .requestMatchers(HttpMethod.POST, "/api/inventory").hasRole("STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("STAFF")
                .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasRole("STAFF")

                .requestMatchers(HttpMethod.POST, "/api/announcements").hasRole("STAFF")

                .requestMatchers(HttpMethod.GET, "/api/transfers", "/api/transfers/**").hasRole("STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/transfers/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/transfers/**").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/donations/**").hasRole("STAFF")

                // =============================
                // Everything else requires authentication
                // =============================
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}