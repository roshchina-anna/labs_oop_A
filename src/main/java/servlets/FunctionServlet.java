package servlets;

import DTO.Function;
import DTO.Point;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.PointRepository;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@WebServlet("/api/functions/*")
public class FunctionServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(FunctionServlet.class);
    private FunctionRepository functionRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private PointRepository pointRepository;

    @Override
    public void init() {
        this.functionRepository = new FunctionRepository();
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
        this.pointRepository = new PointRepository();
    }
    // формирование тела (в формате JSON)
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(error));
        out.flush();
    }
    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
    // аутентификация
    private User authenticate(HttpServletRequest req) {
        return ServletHelper.authenticateUser(req, userRepository);
    }

    @Override
    // (GET /api/functions)
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) { // проверка аутентификации
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }

        // /api/functions — получить все функции (доступ - ADMIN)
        if (pathInfo == null || "/".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<Function> function = functionRepository.findAll();
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(function));
            logger.info("Отправлены все функции");
            return;
        }
        // убираем начальный '/'
        pathInfo = pathInfo.substring(1);

        // /api/functions/sorted - получить отсортированные функции
        if ("sorted".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) { // (доступ - ADMIN)
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<Function> function = functionRepository.findAllSortedByName();
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(function));
            logger.info("Отправлены отсортированные функции");
            return;
        }

        // /api/functions/user/{userId} - получить все функции пользователя
        if (pathInfo.startsWith("user/")) { // (доступ ADMIN или пользователь)
            String userIdStr = pathInfo.substring("user/".length());
            try {
                int userId = Integer.parseInt(userIdStr);
                if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                    return;
                }
                List<Function> function = functionRepository.findByUserId(userId);
                resp.setContentType("application/json");
                resp.getWriter().print(objectMapper.writeValueAsString(function));
                logger.info("Отправлены функции пользователя {}", userId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId"); // 400
                return;
            }
        }
        // /api/functions/{id} - получить функцию по id
        // (доступ - ADMIN или владелец)
        try {
            int id = Integer.parseInt(pathInfo);
            Function f = functionRepository.findById(id);
            if (f == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            if (!Objects.equals(f.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(f));
            logger.info("Отправлена функция {}", id);
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }

    @Override
    // (POST /api/functions) - создание новой функции
    // (доступ - любой авторизированный пользователь)
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        String body = readBody(req);
        try {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("points")) {
            createFunctionWithPoints(authUser, resp, root);
            } else {
                Function f = objectMapper.treeToValue(root, Function.class);
                if (f.getName() == null || f.getName().trim().isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name is required"); // 400
                    return;
                }
                if (f.getExpression() == null || f.getExpression().trim().isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expression is required"); // 400
                    return;
                }
                f.setUserId(authUser.getId()); // текущий пользователь
                Integer id = functionRepository.insert(f);
                if (id != null) {
                    f.setId(id);
                    resp.setStatus(HttpServletResponse.SC_CREATED); // 201
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(f));
                    logger.info("Функция создана");
                } else {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Insert failed"); // 500
                }
            }
        } catch (JsonProcessingException e) {
            String message = e.getOriginalMessage() == null ? "Invalid JSON format" : e.getOriginalMessage();
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, message); // 400
            logger.warn("Некорректный JSON при создании функции: {}", message);
        } catch (Exception e) {
            logger.error("Ошибка при создании функции", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error"); // 500
        }
    }

    @Override
    // (PUT /api/functions/{id}) - обновление функции (доступ - владелец или ADMIN)
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function ID required"); // 400
            return;
        }
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Function existing = functionRepository.findById(id);
            if (existing == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            // либо владелец, либо ADMIN
            if (!Objects.equals(existing.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            ObjectNode update = (ObjectNode) objectMapper.readTree(readBody(req));
            Integer requestedUserId = null;
            if (update.has("userId") && !update.get("userId").isNull()) {
                if (!update.get("userId").canConvertToInt()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId"); // 400
                    return;
                }
                requestedUserId = update.get("userId").asInt();
            }
            // обновление полей
            if (update.has("name") && !update.get("name").isNull()) {
                existing.setName(update.get("name").asText());
            }
            if (update.has("expression") && !update.get("expression").isNull()) {
                existing.setExpression(update.get("expression").asText());
            }
            if (requestedUserId != null && !Objects.equals(existing.getUserId(), requestedUserId)) {
                if (!"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN can change userId"); // 403
                    return;
                }
                User newOwner = userRepository.findById(requestedUserId);
                if (newOwner == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found"); // 404
                    return;
                }
                existing.setUserId(requestedUserId);
            }
            List<FunctionWithPointsRequest.PointPayload> providedPoints = null;
            try {
                if (update.has("points") && update.get("points").isArray()) {
                    providedPoints = parsePointPayload(update.get("points"));
                }
            } catch (JsonProcessingException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getOriginalMessage());
                return;
            }
            if (functionRepository.update(existing)) {
                if (providedPoints != null) {
                    pointRepository.replacePoints(id, toPointEntities(id, providedPoints));
                }
                Function updated = functionRepository.findById(id);
                if (updated == null) {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load updated function"); // 500
                    return;
                }
                resp.setContentType("application/json");
                if (providedPoints != null) {
                    resp.getWriter().print(objectMapper.writeValueAsString(new FunctionWithPointsResponse(
                            updated.getId(),
                            updated.getName(),
                            updated.getExpression(),
                            updated.getUserId(),
                            providedPoints
                    )));
                } else {
                    resp.getWriter().print(objectMapper.writeValueAsString(updated));
                }
                logger.info("Обновлена функция {}", id);
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update failed"); // 500
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }

    @Override
    // (DELETE /api/functions/{id}) - удаление функции
    // доступ - владелец или ADMIN
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function ID required"); // 400
            return;
        }
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Function f = functionRepository.findById(id);
            if (f == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 400
                return;
            }
            // либо ADMIN, либо владелец
            if (!Objects.equals(f.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            if (functionRepository.delete(id)) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
                logger.info("Удалена функция {}", id);
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Delete failed"); // 500
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }
    private void createFunctionWithPoints(User authUser, HttpServletResponse resp, JsonNode root) throws IOException {
        FunctionWithPointsRequest request = objectMapper.treeToValue(root, FunctionWithPointsRequest.class);
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name is required");
            return;
        }
        if (request.getExpression() == null || request.getExpression().trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expression is required");
            return;
        }
        List<FunctionWithPointsRequest.PointPayload> points = parsePointPayload(root.get("points"));
        Function function = new Function(request.getName(), request.getExpression(), authUser.getId());
        Integer id = functionRepository.insert(function);
        if (id == null) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Insert failed");
            return;
        }
        function.setId(id);
        pointRepository.replacePoints(id, toPointEntities(id, points));
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");
        resp.getWriter().print(objectMapper.writeValueAsString(
            new FunctionWithPointsResponse(function.getId(), function.getName(), function.getExpression(), authUser.getId(), points)
        ));
        logger.info("Создана функция с {} точками", points.size());
    }

    private List<FunctionWithPointsRequest.PointPayload> parsePointPayload(JsonNode node) throws IOException {
        if (node == null || !node.isArray()) {
            throw new JsonProcessingException("Поле points должно быть массивом точек") { };
        }
        List<FunctionWithPointsRequest.PointPayload> points = new ArrayList<>();
        for (JsonNode pointNode : node) {
            FunctionWithPointsRequest.PointPayload payload = objectMapper.treeToValue(pointNode, FunctionWithPointsRequest.PointPayload.class);
            if (payload.getXValue() == null || payload.getYValue() == null) {
                throw new JsonProcessingException("Point must contain xValue and yValue") { };
            }
            points.add(payload);
        }
        if (points.size() < 2) {
            throw new JsonProcessingException("Нужно минимум две точки для функции") { };
        }
        return points;
    }

    private List<Point> toPointEntities(Integer functionId, List<FunctionWithPointsRequest.PointPayload> payloads) {
        List<Point> entities = new ArrayList<>();
        for (FunctionWithPointsRequest.PointPayload payload : payloads) {
            entities.add(new Point(functionId, payload.getXValue(), payload.getYValue()));
        }
        return entities;
    }
}