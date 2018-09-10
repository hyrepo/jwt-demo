package com.devhao.springdojo.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class ApiResource {
    private final static String JWT_HEADER_NAME = "Authorization";

    @GetMapping("/api")
    public String testApi(HttpServletRequest request, HttpServletResponse response) {
        String oldJwt = request.getHeader(JWT_HEADER_NAME);
        String newJwt = response.getHeader(JWT_HEADER_NAME);

        return String.format("Your old JWT is:\n%s \nYour new JWT is:\n%s\n", oldJwt, newJwt);
    }
}
