package com.devhao.springdojo.service;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class JwtServiceTest {
    private JwtService jwtService = new JwtService();

    @Test
    public void shouldParseToken() {
        String payload = "test";
        String token = jwtService.generateToken(payload);

        assertThat(jwtService.parseToken(token), is(payload));
    }

    @Test
    public void shouldValidateValidToken() {
        String payload = "test";
        String token = jwtService.generateToken(payload);

        assertThat(jwtService.isTokenValid(token), is(true));
    }

    @Test
    public void shouldValidateInvalidToken() {
        String payload = "test";
        String token = jwtService.generateToken(payload) + "hack";

        assertThat(jwtService.isTokenValid(token), is(false));
    }
}