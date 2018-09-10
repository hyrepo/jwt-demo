package com.devhao.springdojo.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtService {
    private static final String SECRET_KEY = "This Is Secret Key";
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long TIME_OUT_SECOND = 60 * MILLIS_PER_SECOND;

    public String generateToken(String payload) {
        return Jwts.builder()
                .setSubject(payload)
                .setExpiration(new Date(System.currentTimeMillis() + TIME_OUT_SECOND))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String parseToken(String jwt) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String jwt) {
        try {
            parseToken(jwt);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
}
