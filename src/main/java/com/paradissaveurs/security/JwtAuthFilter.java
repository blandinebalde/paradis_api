package com.paradissaveurs.security;

import com.paradissaveurs.repository.AdminUserRepository;
import com.paradissaveurs.service.TokenSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;
    private final TokenSessionService tokenSessionService;

    public JwtAuthFilter(JwtService jwtService, AdminUserRepository adminUserRepository,
                         TokenSessionService tokenSessionService) {
        this.jwtService = jwtService;
        this.adminUserRepository = adminUserRepository;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isSignatureValid(token) && jwtService.isNotExpired(token)) {
                String jti = jwtService.extractJti(token);
                if (tokenSessionService.isActive(jti)) {
                    String username = jwtService.extractUsername(token);
                    adminUserRepository.findByUsername(username).ifPresent(user -> {
                        if (user.isActive()) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    username,
                                    jti,
                                    AuthHelper.authoritiesFor(user)
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    });
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
