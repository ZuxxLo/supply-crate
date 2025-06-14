package org.sc.msgateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.sc.commonconfig.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        System.out.println(path);
        boolean isPublicGetEndpoint =
                (path.equals("/api/products") || path.matches("/api/products/\\d+")
                        || path.equals("/api/categories") || path.matches("/api/categories/\\d+"))
                        && method.equalsIgnoreCase("GET");

        boolean isAuthFreeEndpoint =
                path.contains("/api/users/login") ||
                        path.contains("/api/users/register") ||
                        path.contains("/oauth2") || path.startsWith("/search") || path.startsWith("/suggest");

        boolean isSwaggerEndpoint =
                path.equals("/swagger-ui.html") ||
                        path.startsWith("/swagger-ui/") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/service-auth/v3/api-docs") ||
                        path.startsWith("/service-products/v3/api-docs");

        if (isPublicGetEndpoint || isAuthFreeEndpoint || isSwaggerEndpoint) {
            return chain.filter(exchange);
        }


        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("ddddddd");
            return unauthorized(exchange);
        }

        try {
            Claims claims = jwtUtil.validateToken(header.substring(7));
            System.out.println(claims);
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email").toString())
                    .header("X-Roles", claims.get("roles").toString())
                    .build();
            System.out.println(claims);
            System.out.println("claimsclaimsclaimsclaims");
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (JwtException e) {
            return unauthorized(exchange);
        }
    }


    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        System.out.println("----------unauthorizedunauthorizedunauthorized--------");
        return exchange.getResponse().setComplete();
    }
}
