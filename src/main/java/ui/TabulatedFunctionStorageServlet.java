package ui;

import DTO.Function;
import DTO.Point;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.PointRepository;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import io.FunctionsIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlets.ServletHelper;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@WebServlet("/ui/storage/*")
public class TabulatedFunctionStorageServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UiExceptionHandler exceptionHandler = new UiExceptionHandler();

    private final PointRepository pointRepository = new PointRepository();
    private final FunctionRepository functionRepository = new FunctionRepository();
    private final UserRepository userRepository = new UserRepository();
    private static final String TABULATED_EXPRESSION = "tabulated-ui";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/functions".equals(path)) {
            sendSavedFunctions(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            switch (path) {
                case "/export" -> handleExport(req, resp);
                case "/import" -> handleImport(req, resp);
                case "/functions" -> handleSave(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            exceptionHandler.handle(e, resp);
        }
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (!"/functions".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = authenticate(req, resp);
        if (user == null) {
            return;
        }
        List<Function> functions = functionRepository.findByUserId(user.getId());
        for (Function function : functions) {
            if (!Objects.equals(function.getExpression(), TABULATED_EXPRESSION)) {
                continue;
            }
            pointRepository.deleteByFunctionId(function.getId());
            functionRepository.delete(function.getId());
        }
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
    private void handleExport(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ExportRequest request = objectMapper.readValue(req.getInputStream(), ExportRequest.class);
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            throw new IllegalArgumentException("Выберите функцию для сохранения");
        }
        String format = normalizeFormat(request.getFormat());
        TabulatedFunction function = buildFunctionFromPoints(request.getPoints(), request.getFactoryType());

        StringWriter writer = new StringWriter();
        String filename = (request.getName() == null || request.getName().isBlank())
                ? "tabulated-function" : request.getName();
        switch (format) {
            case "json" -> FunctionsIO.writeTabulatedFunctionJson(writer, function);
            case "xml" -> FunctionsIO.writeTabulatedFunctionXml(writer, function);
            default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + request.getFormat());
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        if ("json".equals(format)) {
            resp.setContentType("application/json");
        } else {
            resp.setContentType("application/xml");
        }
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '.' + format + "\"");
        resp.getWriter().write(writer.toString());
    }

    private void handleImport(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String format = normalizeFormat(req.getParameter("format"));
        String factoryType = req.getParameter("factoryType");
        String payload = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (payload.isBlank()) {
            throw new IllegalArgumentException("Передайте содержимое файла для загрузки");
        }
        TabulatedFunctionFactory factory = resolveFactory(factoryType);
        TabulatedFunction function;
        try (StringReader reader = new StringReader(payload)) {
            function = switch (format) {
                case "json" -> FunctionsIO.readTabulatedFunctionJson(reader, factory);
                case "xml" -> FunctionsIO.readTabulatedFunctionXml(reader, factory);
                default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
            };
        }
        respondWithFunction(resp, function, "Импорт " + format.toUpperCase());
    }

    private void sendSavedFunctions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = authenticate(req, resp);
        if (user == null) {
            return;
        }
        List<Function> userFunctions = functionRepository.findByUserId(user.getId());
        List<SavedFunctionResponse> payload = new ArrayList<>();
        for (Function function : userFunctions) {
            if (!Objects.equals(function.getExpression(), TABULATED_EXPRESSION)) {
                continue;
            }
            List<Point> points = pointRepository.findByFunctionId(function.getId());
            points.sort(Comparator.comparing(Point::getXValue));
            payload.add(new SavedFunctionResponse(function.getId(), function.getName(), toUiPoints(points)));
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private void handleSave(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = authenticate(req, resp);
        if (user == null) {
            return;
        }
        SavedFunctionRequest request = objectMapper.readValue(req.getInputStream(), SavedFunctionRequest.class);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Введите название сохранения");
        }
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            throw new IllegalArgumentException("Нужно минимум две точки");
        }
        Function target = findExistingFunction(user.getId(), request.getName());
        if (target == null) {
            target = new Function(request.getName(), TABULATED_EXPRESSION, user.getId());
            Integer id = functionRepository.insert(target);
            target.setId(id);
        } else {
            pointRepository.deleteByFunctionId(target.getId());
        }
        for (UiPoint point : request.getPoints()) {
            pointRepository.insert(new Point(target.getId(), point.x(), point.y()));
        }
        SavedFunctionResponse response = new SavedFunctionResponse(target.getId(), target.getName(), request.getPoints());
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(response));
    }

    private Function findExistingFunction(Integer userId, String name) {
        List<Function> functions = functionRepository.findByUserId(userId);
        for (Function function : functions) {
            if (!Objects.equals(function.getExpression(), TABULATED_EXPRESSION)) {
                continue;
            }
            if (function.getName() != null && function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }

    private List<UiPoint> toUiPoints(List<Point> points) {
        List<UiPoint> result = new ArrayList<>();
        for (Point point : points) {
            result.add(new UiPoint(point.getXValue(), point.getYValue()));
        }
        return result;
    }

    private TabulatedFunction buildFunctionFromPoints(List<UiPoint> points, String factoryType) {
        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            UiPoint point = points.get(i);
            xValues[i] = point.x();
            yValues[i] = point.y();
        }
        return resolveFactory(factoryType).create(xValues, yValues);
    }

    private void respondWithFunction(HttpServletResponse resp, TabulatedFunction function, String source) throws IOException {
        List<UiPoint> points = new ArrayList<>();
        for (functions.Point point : function) {
            points.add(new UiPoint(point.x, point.y));
        }
        TabulatedFunctionResponse response = new TabulatedFunctionResponse(source, points);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(response));
    }

    private TabulatedFunctionFactory resolveFactory(String factoryType) {
        if (factoryType == null || factoryType.isBlank()) {
            return new ArrayTabulatedFunctionFactory();
        }
        return switch (factoryType.toLowerCase()) {
            case "list", "linked", "linkedlist", "linked-list" -> new LinkedListTabulatedFunctionFactory();
            default -> new ArrayTabulatedFunctionFactory();
        };
    }
    private String normalizeFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return "json";
        }
        return raw.trim().toLowerCase();
    }

    private User authenticate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = ServletHelper.authenticateUser(req, userRepository);
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        return user;
    }
}