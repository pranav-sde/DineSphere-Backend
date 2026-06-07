package com.festora.monolith.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_URI = "uri";
    private static final String MDC_METHOD = "method";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 1. Establish or extract traceId
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // 2. Put metadata into MDC
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_URI, request.getRequestURI());
        MDC.put(MDC_METHOD, request.getMethod());

        // 3. Inject traceId in Response Headers for client-side correlation
        response.setHeader(HEADER_TRACE_ID, traceId);

        // 4. Log request start
        String queryString = request.getQueryString();
        String params = (queryString != null) ? "?" + queryString : "";
        log.info("Incoming Request: {} {}{} | IP: {}", request.getMethod(), request.getRequestURI(), params, request.getRemoteAddr());

        try {
            // 5. Execute downstream filters/controllers
            filterChain.doFilter(request, response);
        } finally {
            // 6. Measure execution duration
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // 7. Structured log for request end based on HTTP status code
            if (status >= 500) {
                log.error("Request Finished with SERVER ERROR: {} {} | Status: {} | Duration: {}ms",
                        request.getMethod(), request.getRequestURI(), status, duration);
            } else if (status >= 400) {
                log.warn("Request Finished with CLIENT ERROR: {} {} | Status: {} | Duration: {}ms",
                        request.getMethod(), request.getRequestURI(), status, duration);
            } else {
                log.info("Request Finished Successfully: {} {} | Status: {} | Duration: {}ms",
                        request.getMethod(), request.getRequestURI(), status, duration);
            }

            // 8. Clean up MDC context to prevent ThreadLocal memory leaks
            MDC.clear();
        }
    }
}
