package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebServlet("/api/users")
public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    // (POST /api/users) - доступ общий (без авторизации)
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Получен запрос на регистрацию нового пользователя");
        // чтение тела
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(sb.toString());

            // проверка обязательных полей
            if (!jsonNode.has("username") || !jsonNode.has("password")) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Требуются поля: username и password"); // 400
                return;
            }

            String username = jsonNode.get("username").asText().trim();
            String plainPassword = jsonNode.get("password").asText();

            if (username.isEmpty() || plainPassword.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "username и password не могут быть пустыми"); // 400
                return;
            }
            // проверка существует ли уже пользователь с таким именем
            User existing = userRepository.findByUsername(username);
            if (existing != null) {
                sendError(resp, HttpServletResponse.SC_CONFLICT,
                        "Пользователь с таким именем уже существует"); // 409
                return;
            }
            // хеширование
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(hashedPassword);
            user.setRole("USER");

            // сохранение в бд
            Integer id = userRepository.insert(user);
            if (id == null) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Ошибка при создании пользователя");
                return;
            }
            user.setId(id);
            user.setPasswordHash(null);
            resp.setStatus(HttpServletResponse.SC_CREATED); // возврат статуса
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            // тело ответа
            String responseJson = objectMapper.writeValueAsString(user);
            try (PrintWriter writer = resp.getWriter()) {
                writer.print(responseJson);
            }
            logger.info("пользователь создан");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Неверный формат JSON");  // 400
        } catch (Exception e) {
            logger.error("Ошибка при регистрации", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Внутренняя ошибка сервера"); // 500
        }
    }

    // метод для возврата ошибок (формат JSON)
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