package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserServlet.class);
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
        logger.info("UserServlet инициализирован");
    }
    // метод для аутентификации пользователя через basic auth
    private User authenticate(HttpServletRequest req) {
        return ServletHelper.authenticateUser(req, userRepository);
    }
    // формирование тела
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status); // возврат статуса
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // тело ответа + описание ошибки
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(error));
        out.flush();
        logger.warn("Ошибка {}: {}", status, message);
    }

    @Override
    // (GET /api/users)
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        // проверка аутентификации
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        // /api/users/me — текущий пользователь
        if ("/me".equals(pathInfo)) {
            authUser.setPasswordHash(null);
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(authUser));
            logger.info("Отправлены данные текущего пользователя");
            return;
        }
        // /api/users — получить список всех пользователей (доступ - ADMIN)
        if (pathInfo == null || "/".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<User> users = userRepository.findAll();
            users.forEach(u -> u.setPasswordHash(null));
            // тело ответа со списком
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(users));
            logger.info("Отправлен список {} пользователей", users.size());
            return;
        }
        // обрезаем начальный '/'
        pathInfo = pathInfo.substring(1);

        // /api/users/sorted - получить отсортированный список
        if ("sorted".equals(pathInfo)) { // доступ - ADMIN
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<User> users = userRepository.findAllSortedByUsername();
            users.forEach(u -> u.setPasswordHash(null));
            // тело ответа с отсортированным списком
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(users));
            logger.info("Отправлен отсортированный список пользователей");
            return;
        }

        // /api/users/{id} - получение конкретного пользователя по id
        try { // доступ - пользователь или ADMIN
            int id = Integer.parseInt(pathInfo);
            if (id != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            User user = userRepository.findById(id);
            if (user == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found"); // 404
                return;
            }
            user.setPasswordHash(null);
            // тело ответа с данными пользователя
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(user));
            logger.info("Отправлен пользователь ID {}", id);
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID"); // 400
        }
    }

    @Override
    // (PUT /api/users/{id}) - обновление пользователя (доступ - пользователь или ADMIN)
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }

        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "User ID required"); // 400
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            if (id != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            User existing = userRepository.findById(id);
            if (existing == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found"); // 404
                return;
            }
            //чтение тела запроса
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            ObjectNode update = (ObjectNode) objectMapper.readTree(sb.toString());
            // обновление полей
            if (update.has("username") && !update.get("username").isNull()) {
                existing.setUsername(update.get("username").asText());
            }
            if (update.has("passwordHash") && !update.get("passwordHash").isNull()) {
                String rawPassword = update.get("passwordHash").asText();
                String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
                existing.setPasswordHash(hashedPassword);
            }
            if (update.has("role") && !update.get("role").isNull() && "ADMIN".equals(authUser.getRole())) {
                existing.setRole(update.get("role").asText());
            }
            if (userRepository.update(existing)) {
                existing.setPasswordHash(null);
                // тело ответа с обновленными данными
                resp.setContentType("application/json");
                resp.getWriter().print(objectMapper.writeValueAsString(existing));
                logger.info("Пользователь {} обновлён", id);
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update failed"); // 500
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID"); // 400
        }
    }

    @Override
    // (DELETE /api/users/{id}) - удаление пользователя
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 40
            return;
        }
        // доступ - ADMIN
        if (!"ADMIN".equals(authUser.getRole())) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN can delete users"); // 403
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "User ID required"); // 400
            return;
        }
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            if (userRepository.delete(id)) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 (нет тела)
                logger.info("Пользователь {} удалён", id);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found"); // 404
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID"); // 400
        }
    }
}