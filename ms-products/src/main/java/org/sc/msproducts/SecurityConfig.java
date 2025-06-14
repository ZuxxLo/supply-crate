package org.sc.msproducts;


import jakarta.ws.rs.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*").permitAll()
                         .requestMatchers("/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(preAuthFilter(), AbstractPreAuthenticatedProcessingFilter.class);

        return http.build();
    }

    @Value("${gateway.secret.key}")
    private String gatewaySecret;

    private static final String HEADER_NAME = "X-GATEWAY-AUTHORIZED";

    @Bean
    public OncePerRequestFilter preAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {


                String header = request.getHeader(HEADER_NAME);

                if (!gatewaySecret.equals(header)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Forbidden: Direct access not allowed.\"}");
                    return;
                }

                System.out.println("requestrequestrequestrequest");
                System.out.println(request.getHeaderNames());
                String userId = request.getHeader("X-User-Id");
                String rolesHeader = request.getHeader("X-Roles");

                if (userId != null && !userId.isEmpty()) {
                    List<SimpleGrantedAuthority> authorities = rolesHeader != null
                            ? Arrays.stream(rolesHeader.replace("[", "").replace("]", "").split(","))
                            .map(String::trim)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList())
                            : List.of();
                    System.out.println("authoritiesauthoritiesauthoritiesauthorities");

                    System.out.println(authorities);

                    UserDetails userDetails = new User(userId, "", authorities);
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            );
                }
                System.out.println("rolesHeaderrolesHeaderrolesHeaderrolesHeader");

                System.out.println(rolesHeader);

                filterChain.doFilter(request, response);
            }
        };
    }
}