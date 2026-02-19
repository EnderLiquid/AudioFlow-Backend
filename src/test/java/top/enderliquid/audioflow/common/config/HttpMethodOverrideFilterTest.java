package top.enderliquid.audioflow.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HttpMethodOverrideFilterTest {

    private HttpMethodOverrideFilter filter;
    private FilterChain filterChain;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new HttpMethodOverrideFilter();
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldOverrideMethodWhenHeaderPresentInPost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-HTTP-Method-Override", "PUT");

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertEquals("PUT", requestCaptor.getValue().getMethod());
    }

    @Test
    void shouldHandleCaseInsensitiveOverrideValue() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-HTTP-Method-Override", "patch");

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertEquals("PATCH", requestCaptor.getValue().getMethod());
    }

    @Test
    void shouldNotOverrideWhenMethodIsNotPost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-HTTP-Method-Override", "DELETE");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("GET", request.getMethod());
    }

    @Test
    void shouldNotOverrideWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("POST", request.getMethod());
    }

    @Test
    void shouldNotOverrideWhenMethodIsNotAllowed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("X-HTTP-Method-Override", "TRACE");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("POST", request.getMethod());
    }
}
