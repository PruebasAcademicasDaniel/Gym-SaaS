package com.gymflow.auth.infrastructure.security;

import com.gymflow.shared.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida el Bearer token en cada request, puebla el SecurityContext y el
 * TenantContext (gymId — null para SUPER_ADMIN). Stateless: no consulta la
 * base, todo sale de los claims del JWT.
 *
 * El finally es obligatorio: Tomcat reutiliza threads entre requests, y
 * sin limpiar el TenantContext acá, un request de otro tenant que caiga en
 * el mismo thread heredaría el tenant del request anterior.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith(BEARER_PREFIX)) {
                try {
                    AuthenticatedPrincipal principal = jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()));
                    var authority = new SimpleGrantedAuthority("ROLE_" + principal.role().name());
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContext.setCurrentTenantId(principal.gymId());
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                }
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
