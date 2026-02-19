package top.enderliquid.audioflow.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Filter that detects the X-HTTP-Method-Override header in POST requests
 * and wraps the request to return the overridden method.
 */
public class HttpMethodOverrideFilter extends OncePerRequestFilter {

    private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    private static final Set<String> ALLOWED_METHODS = Set.of("PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String methodOverride = request.getHeader(X_HTTP_METHOD_OVERRIDE);
            if (methodOverride != null) {
                String method = methodOverride.toUpperCase(Locale.ENGLISH);
                if (ALLOWED_METHODS.contains(method)) {
                    HttpServletRequest wrapper = new HttpMethodRequestWrapper(request, method);
                    filterChain.doFilter(wrapper, response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {
        private final String method;

        public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
            super(request);
            this.method = method;
        }

        @Override
        public String getMethod() {
            return this.method;
        }
    }
}
