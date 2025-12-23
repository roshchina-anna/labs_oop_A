package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String ISSUER = "labs_oop";
    private static final long EXPIRATION_MS = Duration.ofHours(24).toMillis();
    private static final String SECRET = resolveSecret();

    private JwtUtil() {
    }

    private static String resolveSecret() {
        String secretFromEnv = System.getenv("JWT_SECRET");
        if (secretFromEnv != null && !secretFromEnv.isBlank()) {
            return secretFromEnv;
        }
        logger.warn("Using fallback JWT secret; set JWT_SECRET for production");
        return "change-me-secret";
    }

    public static String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(EXPIRATION_MS)))
                .withClaim("userId", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole())
                .sign(algorithm);
    }

    public static User validateToken(String token, UserRepository userRepository) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            Integer userId = jwt.getClaim("userId").asInt();
            if (userId == null) {
                logger.warn("Token is missing userId claim");
                return null;
            }
            User user = userRepository.findById(userId);
            if (user == null) {
                logger.warn("Token refers to non-existent user {}", userId);
            }
            return user;
        } catch (JWTVerificationException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Failed to validate JWT token", e);
            return null;
        }
    }
}