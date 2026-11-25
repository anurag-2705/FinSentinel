package com.finsentinel.security;

import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.role.repository.UserRoleRepository;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            String token = authorizationHeader.substring(7);

            if (jwtService.isTokenValid(token)) {

                Claims claims = jwtService.extractClaims(token);

                UUID userId = UUID.fromString(
                        claims.get("userId", String.class)
                );

                AppUser user = userRepository.findById(userId)
                        .orElse(null);

                if (user != null
                        && user.getStatus() == UserStatus.ACTIVE) {

                    List<SimpleGrantedAuthority> authorities =
                            userRoleRepository.findRoleNamesByUserId(userId)
                                    .stream()
                                    .map(roleName -> new SimpleGrantedAuthority(
                                            "ROLE_" + roleName
                                    ))
                                    .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}