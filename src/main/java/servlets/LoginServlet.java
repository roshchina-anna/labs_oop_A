package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Получен запрос на авторизацию пользователя");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(sb.toString());
            if (!jsonNode.has("username") || !jsonNode.has("password")) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Требуются поля: username и password");
                return;
            }

            String username = jsonNode.get("username").asText().trim();
            String plainPassword = jsonNode.get("password").asText();

            User user = userRepository.findByUsername(username);
            if (user == null || !BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Неверные учетные данные");
                return;
            }

            String token = JwtUtil.generateToken(user);
            user.setPasswordHash(null);

            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("token", token);
            responseJson.set("user", objectMapper.valueToTree(user));

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter writer = resp.getWriter()) {
                writer.print(objectMapper.writeValueAsString(responseJson));
            }
            logger.info("Пользователь {} успешно авторизован", username);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Неверный формат JSON");
        } catch (Exception e) {
            logger.error("Ошибка при авторизации", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String errorJson = String.format("{\"error\": \"%s\"}", message);
        try (PrintWriter writer = resp.getWriter()) {
            writer.print(errorJson);
        }
        logger.warn("Ошибка {}: {}", status, message);
    }
}