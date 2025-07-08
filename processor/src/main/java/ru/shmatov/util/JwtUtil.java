package ru.shmatov.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated JWT for user: {}", user.getUsername());
        return token;
    }

    public String extractUsername(String token) {
        String username = getClaims(token).getSubject();
        log.debug("Extracted username from token: {}", username);
        return username;
    }

    public boolean isValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        boolean valid = username.equals(userDetails.getUsername()) && !isExpired(token);
        log.debug("JWT validation result for {}: {}", username, valid);
        return valid;
    }

    private boolean isExpired(String token) {
        boolean expired = getClaims(token).getExpiration().before(new Date());
        log.debug("Token expiration check: {}", expired);
        return expired;
    }

    private Claims getClaims(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        log.debug("Parsed claims: {}", claims);
        return claims;
    }
}
