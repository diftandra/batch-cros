package id.co.bca.crosbatch.api;

import id.co.bca.crosbatch.config.CrosBatchProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Rejects /api/* requests without the configured API key header (HTTP 401). */
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String header;
    private final byte[] expectedKey;

    public ApiKeyFilter(CrosBatchProperties.Api api) {
        if (api.getKey() == null || api.getKey().isBlank()) {
            throw new IllegalStateException("API mode needs an API key: set CROSBATCH_API_KEY (crosbatch.api.key)");
        }
        this.header = api.getKeyHeader();
        this.expectedKey = api.getKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(header);
        // Constant-time comparison, so the key cannot be guessed from response times.
        if (provided == null || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"missing or invalid " + header + " header\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
