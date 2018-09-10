package com.devhao.springdojo.filter;

import com.devhao.springdojo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebFilter(urlPatterns = "/*")
@Component
public class JwtFilter implements Filter {
    private static final List<String> WHITE_LIST = Collections.singletonList("/registration");
    private static final String JWT_HEADER_NAME = "Authorization";
    private JwtService jwtService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String jwt = httpServletRequest.getHeader(JWT_HEADER_NAME);
        if (WHITE_LIST.contains(httpServletRequest.getRequestURI())) {
            chain.doFilter(request, response);
        } else if (isTokenValid(jwt)) {
            updateToken(httpServletResponse, jwt);
            chain.doFilter(request, response);
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void updateToken(HttpServletResponse httpServletResponse, String jwt) {
        String payload = jwtService.parseToken(jwt);
        String newToken = jwtService.generateToken(payload);
        httpServletResponse.setHeader(JWT_HEADER_NAME, newToken);
    }

    private boolean isTokenValid(String token) {
        return jwtService.isTokenValid(token);
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    @Autowired
    public void setJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }
}
