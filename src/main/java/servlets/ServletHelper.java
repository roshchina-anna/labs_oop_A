package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import servlets.JwtUtil;

public class ServletHelper {
    private static final Logger logger = LoggerFactory.getLogger(ServletHelper.class);

    public static boolean isAllowed(String userRole, String... allowedRoles) {
        if (userRole == null) return false;
        for (String role : allowedRoles) {
            if (userRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public static User authenticateUser(HttpServletRequest req, UserRepository userRepository) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            logger.warn("Отсутствует заголовок Authorization");
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            try {
                String username = JwtUtil.getUsernameFromToken(token);
                if (username == null) {
                    logger.warn("Невозможно извлечь пользователя из JWT");
                    return null;
                }
                User user = userRepository.findByUsername(username);
                if (user == null || !JwtUtil.isTokenValid(token, user)) {
                    logger.warn("Неверный JWT токен");
                    return null;
                }
                logger.info("JWT аутентификация успешна: {}", username);
                return user;
            } catch (Exception e) {
                logger.error("Ошибка при проверке JWT", e);
                return null;
            }
        }
        if (authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length != 2) {
                    logger.warn("Неверный формат credentials в Authorization");
                    return null;
                }
                String username = parts[0];
                String plainPassword = parts[1];

                User user = userRepository.findByUsername(username);
                if (user == null) {
                    logger.warn("Пользователь не найден: {}", username);
                    return null;
                }
                String storedHash = user.getPasswordHash();
                if (!BCrypt.checkpw(plainPassword, storedHash)) {
                    logger.warn("Неверный пароль для пользователя: {}", username);
                    return null;
                }
                logger.info("Успешная аутентификация (Basic): {}", username);
                return user;
            } catch (Exception e) {
                logger.error("Ошибка при аутентификации", e);

                return null;
            }
        }
        logger.warn("Неподдерживаемый тип авторизации");
        return null;
    }
}