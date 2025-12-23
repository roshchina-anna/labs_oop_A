package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import functions.MathFunction;
import functions.SqrFunction;
import functions.IdentityFunction;
import functions.UnitFunction;
import functions.ZeroFunction;
import functions.AbstractTabulatedFunction;
import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ui.FunctionOption;
import ui.TabulatedFunctionResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@WebServlet("/api/tabulated/*")
public class TabulatedFunctionServlet extends HttpServlet {
    private static final int MAX_POINT_COUNT = 200;
    private static final int MAX_POINTS_PER_REQUEST = 1000;
    private ObjectMapper objectMapper;
    private Map<String, MathFunction> simpleFunctions;

    @Override
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.simpleFunctions = new TreeMap<>(Comparator.naturalOrder());
        simpleFunctions.put("Единичная функция", new UnitFunction());
        simpleFunctions.put("Квадратичная функция", new SqrFunction());
        simpleFunctions.put("Нулевая функция", new ZeroFunction());
        simpleFunctions.put("Тождественная функция", new IdentityFunction());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || "/options".equals(path)) {
            sendJson(resp, HttpServletResponse.SC_OK, objectMapper.writeValueAsString(buildOptions()));
            return;
        }
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Запрошенный ресурс не найден");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/from-points".equals(path)) {
            handleFromPoints(req, resp);
            return;
        }
        if ("/from-math".equals(path)) {
            handleFromMath(req, resp);
            return;
        }
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Запрошенный ресурс не найден");
    }

    private void handleFromPoints(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body;
        try {
            body = objectMapper.readTree(req.getInputStream());
        } catch (JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный формат запроса");
            return;
        }
        String name = textValue(body, "name");
        String type = textValue(body, "type");
        JsonNode pointsNode = body.get("points");

        if (name.isBlank()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите название функции");
            return;
        }
        if (pointsNode == null || !pointsNode.isArray()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте точки табуляции");
            return;
        }
        if (pointsNode.size() > MAX_POINTS_PER_REQUEST) {
            sendError(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Слишком много точек. Максимум " + MAX_POINTS_PER_REQUEST);
            return;
        }
        if (pointsNode.size() < 2) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Нужно минимум две точки для табуляции");
            return;
        }

        double[] xValues = new double[pointsNode.size()];
        double[] yValues = new double[pointsNode.size()];
        for (int i = 0; i < pointsNode.size(); i++) {
            JsonNode point = pointsNode.get(i);
            if (point.get("x") == null || point.get("y") == null || !point.get("x").isNumber() || !point.get("y").isNumber()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Точки должны содержать числовые значения x и y");
                return;
            }
            xValues[i] = point.get("x").asDouble();
            yValues[i] = point.get("y").asDouble();
        }
        try {
            AbstractTabulatedFunction.checkLengthIsTheSame(xValues, yValues);
            AbstractTabulatedFunction.checkSorted(xValues);
            TabulatedFunction function = chooseFactory(type).create(xValues, yValues);
            sendJson(resp, HttpServletResponse.SC_CREATED,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from(name, storageName(type), function, MAX_POINT_COUNT)));
        } catch (RuntimeException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleFromMath(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body;
        try {
            body = objectMapper.readTree(req.getInputStream());
        } catch (JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный формат запроса");
            return;
        }
        String name = textValue(body, "name");
        String type = textValue(body, "type");
        String functionKey = textValue(body, "functionKey");
        JsonNode countNode = body.get("count");
        JsonNode fromNode = body.get("xFrom");
        JsonNode toNode = body.get("xTo");

        if (name.isBlank()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите название функции");
            return;
        }
        MathFunction mathFunction = simpleFunctions.get(functionKey);
        if (mathFunction == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Выберите функцию для табуляции");
            return;
        }
        if (countNode == null || !countNode.canConvertToInt()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Количество точек должно быть целым числом");
            return;
        }
        if (fromNode == null || toNode == null || !fromNode.isNumber() || !toNode.isNumber()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите числовые границы интервала");
            return;
        }
        int count = countNode.asInt();
        if (count < 2) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Минимум две точки для табуляции");
            return;
        }
        if (count > MAX_POINTS_PER_REQUEST) {
            sendError(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Слишком много точек. Максимум " + MAX_POINTS_PER_REQUEST);
            return;
        }
        double xFrom = fromNode.asDouble();
        double xTo = toNode.asDouble();
        if (xFrom >= xTo) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Левая граница должна быть меньше правой");
            return;
        }
        try {
            TabulatedFunction function = chooseFactory(type).create(mathFunction, xFrom, xTo, count);
            sendJson(resp, HttpServletResponse.SC_CREATED,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from(name, storageName(type), function, MAX_POINT_COUNT)));
        } catch (RuntimeException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private TabulatedFunctionFactory chooseFactory(String type) {
        if ("linked".equalsIgnoreCase(type)) {
            return new LinkedListTabulatedFunctionFactory();
        }
        return new ArrayTabulatedFunctionFactory();
    }

    private String storageName(String type) {
        if ("linked".equalsIgnoreCase(type)) {
            return "Связный список";
        }
        return "Массив";
    }

    private List<FunctionOption> buildOptions() {
        List<FunctionOption> options = new ArrayList<>();
        for (String name : simpleFunctions.keySet()) {
            options.add(new FunctionOption(name));
        }
        return options;
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.print("{\"error\":\"" + message + "\"}");
        out.flush();
    }

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual()) {
            return "";
        }
        return value.asText("");
    }
}