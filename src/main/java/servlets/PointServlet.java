package servlets;

import DTO.Point;
import DTO.Function;
import DTO.User;
import JDBC.repository.PointRepository;
import JDBC.repository.FunctionRepository;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;
import java.util.Objects;

@WebServlet("/api/points/*")
public class PointServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PointServlet.class);
    private PointRepository pointRepository;
    private FunctionRepository functionRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.pointRepository = new PointRepository();
        this.functionRepository = new FunctionRepository();
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
    }

    // метод для проверки доступа к функции
    private boolean hasAccess(int functionId, User user) {
        Function f = functionRepository.findById(functionId);
        return f != null && (Objects.equals(f.getUserId(), user.getId()) || "ADMIN".equals(user.getRole()));
    }
    // формирование тела (возврат ошибки в JSON)
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
    private User authenticate(HttpServletRequest req) {
        return ServletHelper.authenticateUser(req, userRepository);
    }

    @Override
    // (GET /api/points/function)
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        // проверка аутентификации
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path"); // 400
            return;
        }
        // убираем начальный '/'
        pathInfo = pathInfo.substring(1);

        // /api/points/sorted/x - получить все точки отсортированные по х
        // доступ - ADMIN
        if ("sorted/x".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<Point> points = pointRepository.findAllSortedByX();
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(points));
            logger.info("Отправлены точки, сортированные по X");
            return;
        }

        // /api/points/sorted/y - получить все точки отсортированные по у/
        // доступ - ADMIN
        if ("sorted/y".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN");
                return;
            }
            List<Point> points = pointRepository.findAllSortedByY();
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(points));
            logger.info("Отправлены точки, сортированные по Y");
            return;
        }

        // /api/points/function/{functionId} - получить все точки функции
        // доступ - владелец или ADMIN
        if (pathInfo.startsWith("function/") && !pathInfo.contains("/x/")) {
            String functionIdStr = pathInfo.substring("function/".length());
            try {
                int functionId = Integer.parseInt(functionIdStr);
                if (!hasAccess(functionId, authUser)) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                    return;
                }
                List<Point> points = pointRepository.findByFunctionId(functionId);
                resp.setContentType("application/json");
                resp.getWriter().print(objectMapper.writeValueAsString(points));
                logger.info("Отправлены точки функции {}", functionId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId"); // 400
                return;
            }
        }

        // /api/points/function/{functionId}/x/{xValue} - получить конкретную точку по x
        // доступ - ADMIN или владелец
        if (pathInfo.startsWith("function/") && pathInfo.contains("/x/")) {
            String[] parts = pathInfo.split("/x/");
            if (parts.length == 2) {
                try {
                    int functionId = Integer.parseInt(parts[0].substring("function/".length()));
                    double xValue = Double.parseDouble(parts[1]);
                    Function function = functionRepository.findById(functionId);
                    if (function == null) { // проверка существования функции
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                        return;
                    }
                    if (!hasAccess(functionId, authUser)) {
                        sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                        return;
                    }
                    Point p = pointRepository.findByFunctionIdAndX(functionId, xValue);
                    if (p == null) {
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Point not found"); // 404
                        return;
                    }
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(p));
                    logger.info("Отправлена точка function={}, x={}", functionId, xValue);
                    return;
                } catch (NumberFormatException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId or xValue"); // 400
                    return;
                }
            }
        }
        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path"); // 400
    }

    @Override
    // (POST /api/points) - добавление точки
    // доступ - владелец или ADMIN
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        // чтение тела запроса
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        Point p = objectMapper.readValue(sb.toString(), Point.class);
        // проверка обязательных полей
        if (p.getFunctionId() == null || p.getXValue() == null || p.getYValue() == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing fields"); // 400
            return;
        }
        // проверка существования полей
        Function function = functionRepository.findById(p.getFunctionId());
        if (function == null) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
            return;
        }
        // либо владелец, либо ADMIN
        if (!hasAccess(p.getFunctionId(), authUser)) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
            return;
        }
        // проверка на существование точки с таким же значением и id функции
        Point existing = pointRepository.findByFunctionIdAndX(p.getFunctionId(), p.getXValue());
        if (existing != null) {
            sendError(resp, HttpServletResponse.SC_CONFLICT, "Point with this functionId and xValue already exists"); // 409
            return;
        }
        // сохранение в бд
        pointRepository.insert(p);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");
        resp.getWriter().print(objectMapper.writeValueAsString(p));
        logger.info("Создана точка function={}, x={}", p.getFunctionId(), p.getXValue());
    }

    @Override
    // (PUT /api/points/function/{functionId}/x/{xValue}) - обновление точки
    // доступ - владелец или ADMIN
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || !pathInfo.startsWith("/function/")) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path"); // 400
            return;
        }
        // убираем первый '/'
        String cleanPath = pathInfo.substring(1); // убираем первый '/'
        if (!cleanPath.contains("/x/")) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Path must contain /x/"); // 400
            return;
        }
        String[] parts = cleanPath.split("/x/");
        if (parts.length != 2) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path format"); // 400
            return;
        }
        try {
            int functionId = Integer.parseInt(parts[0].substring("function/".length()));
            double xValue = Double.parseDouble(parts[1]);
            // проверка существования функции
            Function function = functionRepository.findById(functionId);
            if (function == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            // либо владелец, либо ADMIN
            if (!hasAccess(functionId, authUser)) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            // чтение тела запроса
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            try {
                ObjectNode node = (ObjectNode) objectMapper.readTree(sb.toString());
                // проверка наличия значения
                if (!node.has("yValue") || node.get("yValue").isNull()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "yValue is required"); // 400
                    return;
                }
                double yValue = node.get("yValue").asDouble();
                // создание и обновление точки
                Point p = new Point(functionId, xValue, yValue);
                // проверка существования точки для обновления
                Point existing = pointRepository.findByFunctionIdAndX(functionId, xValue);
                if (existing == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Point not found"); // 404
                    return;
                }
                if (pointRepository.update(p)) {
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(p));
                    logger.info("Обновлена точка function={}, x={}", functionId, xValue);
                } else {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update failed"); // 500
                }
            } catch (JsonProcessingException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format"); // 400
                logger.warn("Некорректный JSON при обновлении точки: {}", e.getMessage());
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId or xValue"); // 400
        }
    }

    @Override
    // (DELETE /api/points/function/{functionId}/x/{xValue}) - удаление точки
    // доступ - владелец или ADMIN
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || !pathInfo.startsWith("/function/")) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path"); // 400
            return;
        }
        String cleanPath = pathInfo.substring(1);
        if (!cleanPath.contains("/x/")) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Path must contain /x/"); // 400
            return;
        }
        String[] parts = cleanPath.split("/x/");
        if (parts.length != 2) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path format"); // 400
            return;
        }
        try {
            int functionId = Integer.parseInt(parts[0].substring("function/".length()));
            double xValue = Double.parseDouble(parts[1]);
            // проверка существования функции
            Function function = functionRepository.findById(functionId);
            if (function == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            // либо владелец, либо ADMIN
            if (!hasAccess(functionId, authUser)) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            // удаление точки
            if (pointRepository.delete(functionId, xValue)) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                logger.info("Удалена точка function={}, x={}", functionId, xValue);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Point not found"); // 404
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functionId or xValue"); // 400
        }
    }
}