package com.aiwishcoding.easymock.security;

import com.aiwishcoding.easymock.autoconfigure.EasyMockProperties;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;

public class EasyMockAdminFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-EasyMock-Token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

    private final EasyMockProperties properties;
    private final byte[] expectedToken;

    public EasyMockAdminFilter(EasyMockProperties properties) {
        this.properties = properties;
        if (properties.getAdminToken() == null || properties.getAdminToken().trim().length() < 16) {
            throw new IllegalStateException("easymock.admin-token must contain at least 16 characters");
        }
        this.expectedToken = properties.getAdminToken().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = PATH_HELPER.getPathWithinApplication(request);
        if (!isManagedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!isAuthenticated(request)) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"EasyMock\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "EasyMock authentication required");
            return;
        }
        if (isUnsafeMethod(request.getMethod()) && !isSameOrigin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-origin EasyMock requests are not allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isManagedPath(String path) {
        return isSameOrChildPath(path, properties.getPagePath())
                || isSameOrChildPath(path, properties.getApiPath());
    }

    private boolean isSameOrChildPath(String path, String configuredPath) {
        return path.equals(configuredPath) || path.startsWith(configuredPath + "/");
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                token = authorization.substring(BEARER_PREFIX.length());
            } else if (authorization != null && authorization.startsWith("Basic ")) {
                try {
                    String credentials = new String(Base64.getDecoder().decode(
                            authorization.substring("Basic ".length())), StandardCharsets.UTF_8);
                    int separator = credentials.indexOf(':');
                    if (separator >= 0 && "easymock".equals(credentials.substring(0, separator))) {
                        token = credentials.substring(separator + 1);
                    }
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
        }
        return token != null && MessageDigest.isEqual(
                expectedToken, token.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isUnsafeMethod(String method) {
        return !Arrays.asList("GET", "HEAD", "OPTIONS").contains(method);
    }

    private boolean isSameOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isEmpty()) {
            return true;
        }
        try {
            URI uri = URI.create(origin);
            String scheme = request.getScheme();
            int port = uri.getPort() == -1
                    ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
            return uri.getScheme().equalsIgnoreCase(scheme)
                    && uri.getHost().equalsIgnoreCase(request.getServerName())
                    && port == request.getServerPort();
        } catch (Exception e) {
            return false;
        }
    }
}
