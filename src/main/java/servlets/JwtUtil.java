package servlets;

import DTO.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "ChangeThisSecretKeyToSomethingSaferChangeThis";
    private static final long EXPIRATION_MS = 1000L * 60 * 60; // 1 час

    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isTokenValid(String token, User user) {
        try {
            Claims claims = parseToken(token).getBody();
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            return username != null && username.equals(user.getUsername()) && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private static Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }
}