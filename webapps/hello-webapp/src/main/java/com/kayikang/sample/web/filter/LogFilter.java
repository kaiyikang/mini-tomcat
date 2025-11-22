package com.kaiyikang.sample.web.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

@WebFilter(urlPatterns = "/*")
public class LogFilter implements Filter {

    Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        logger.info("{}: {}", req.getMethod(), req.getRequestURI());

        // Go to the next
        chain.doFilter(request, response);
    }

}
