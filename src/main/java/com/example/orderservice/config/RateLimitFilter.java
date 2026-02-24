package com.example.orderservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException{
        String ip = request.getRemoteAddr();


        RequestCounter counter = requestCounts.computeIfAbsent(ip,
                k -> new RequestCounter());

        synchronized (counter){
            long now = Instant.now().getEpochSecond();

            if(now - counter.startTime > WINDOW_SECONDS){
                counter.startTime = now;
                counter.count = 0;
            }
            counter.count++;

            if(counter.count > MAX_REQUESTS){
                response.getWriter().write("Too many requests");
                return;
            }
        }
        filterChain.doFilter(request, response);

    }
    static class RequestCounter{
        long startTime = Instant.now().getEpochSecond();
        int count = 0;
    }
}
