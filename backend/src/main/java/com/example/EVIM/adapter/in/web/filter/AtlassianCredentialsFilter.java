package com.example.EVIM.adapter.in.web.filter;

import com.example.EVIM.adapter.out.persistence.AtlassianTokenService;
import com.example.EVIM.adapter.out.persistence.entity.AtlassianUserToken;
import com.example.EVIM.application.auth.CredentialsContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
public class AtlassianCredentialsFilter extends OncePerRequestFilter {

    private static final String ATLASSIAN_API_BASE = "https://api.atlassian.com/ex/jira/";

    private final AtlassianTokenService tokenService;

    public AtlassianCredentialsFilter(AtlassianTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String sub = jwt.getSubject();
                Optional<AtlassianUserToken> tokenOpt = tokenService.findBySub(sub);

                if (tokenOpt.isPresent()) {
                    AtlassianUserToken token = tokenOpt.get();

                    if (token.getExpiresAt() != null &&
                            Instant.now().isAfter(token.getExpiresAt().minusSeconds(300))) {
                        try {
                            token = tokenService.refresh(token);
                        } catch (Exception e) {
                            logger.warn("Token Atlassian expirado/revogado para sub=" + sub + ". Removendo.", e);
                            tokenService.deleteBySub(sub);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setHeader("X-Atlassian-Reauth", "required");
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"atlassian_reauth_required\"}");
                            return;
                        }
                    }

                    CredentialsContext.set(new CredentialsContext.JiraCredentials(
                            "Bearer " + token.getAccessToken(),
                            ATLASSIAN_API_BASE + token.getCloudId()
                    ));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            CredentialsContext.clear();
        }
    }
}
