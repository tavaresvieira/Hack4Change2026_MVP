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
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // --- Static frontend (Svelte build) ---
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.html",
                    "/_app/**",
                    "/favicon.ico",
                    "/favicon.png",
                    "/robots.txt"
                ).permitAll()

                // --- Public endpoints ---
                .requestMatchers("/api/auth/**").permitAll()

                // Invite token validation is public (register page needs to prefill)
                .requestMatchers(HttpMethod.GET, "/api/invitations/validate/**").permitAll()

                // Donors browse needs, items, orgs, announcements without logging in
                .requestMatchers(HttpMethod.GET, "/api/needs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organizations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()

                // Donors submit donations without an account — auto-matching fires server-side
                .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()

                // Donors submit financial donations without an account
                .requestMatchers(HttpMethod.POST, "/api/financial-donations").permitAll()

                // --- Admin only ---
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/financial-donations", "/api/financial-donations/**").hasRole("ADMIN")
                .requestMatchers("/api/invitations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/organizations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/organizations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/organizations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/organizations/**").hasRole("ADMIN")

                // --- Shelter Staff (authenticated) ---
                .requestMatchers(HttpMethod.POST, "/api/needs").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/needs/*/fulfill").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inventory").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/inventory/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/announcements").hasAnyRole("STAFF", "ADMIN")

                // Staff see incoming transfers and confirm receipt
                .requestMatchers(HttpMethod.GET, "/api/transfers/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/transfers/**").hasAnyRole("STAFF", "ADMIN")

                // Exchange requests between shelters
                .requestMatchers("/api/exchange-requests", "/api/exchange-requests/**").hasAnyRole("STAFF", "ADMIN")

                // Staff can see donation details
                .requestMatchers(HttpMethod.GET, "/api/donations/**").hasAnyRole("STAFF", "ADMIN")

                // Everything else requires authentication
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