package com.example.backend.filter;

import com.example.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Skip JWT validation for login and public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        
        // Try to get token from Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Remove "Bearer " prefix
        } else {
            // Fallback to sessionId query parameter
            token = request.getParameter("sessionId");
        }
        
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\": false, \"message\": \"Missing or invalid token. Provide Authorization header or sessionId parameter\"}");
            response.setContentType("application/json");
            return;
        }
        
        try {
            if (jwtUtil.validateToken(token)) {
                // Add user information to request attributes for use in controllers
                request.setAttribute("username", jwtUtil.extractUsername(token));
                request.setAttribute("role", jwtUtil.extractRole(token));
                request.setAttribute("department", jwtUtil.extractDepartment(token));
                request.setAttribute("userId", jwtUtil.extractUserId(token));
                
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"Invalid or expired token\"}");
                response.setContentType("application/json");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\": false, \"message\": \"Invalid token format\"}");
            response.setContentType("application/json");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/register") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/register") ||
               path.startsWith("/api/auth/users") ||
               path.startsWith("/api/auth/create-user") ||
               path.startsWith("/api/auth/update-user-status") ||
               path.startsWith("/api/admin/") ||
               path.startsWith("/api/budgets") ||
               path.startsWith("/api/notifications") ||
               path.startsWith("/api/users") ||
               path.startsWith("/api/departments") ||
               path.startsWith("/api/dashboard") ||
               path.startsWith("/api/pdf/") ||
               path.startsWith("/api/po/") ||
               path.startsWith("/api/test/");
    }
}

