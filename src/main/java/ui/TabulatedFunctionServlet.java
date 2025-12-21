package ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import functions.MathFunction;
import functions.Point;
import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import operations.ParallelIntegralCalculator;
import ui.IntegrationRequest;
import ui.IntegralResponse;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/ui/tabulated/*")
public class TabulatedFunctionServlet extends HttpServlet {
    private static final int MAX_POINTS = 500;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UiExceptionHandler exceptionHandler = new UiExceptionHandler();
    private final SimpleFunctionRegistry functionRegistry = new SimpleFunctionRegistry();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path != null && path.equals("/functions")) {
            sendFunctions(resp);
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
                case "/arrays" -> handleArrayCreation(req, resp);
                case "/from-function" -> handleFunctionCreation(req, resp);
                case "/integral" -> handleIntegral(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            exceptionHandler.handle(e, resp);
        }
    }

    private void sendFunctions(HttpServletResponse resp) throws IOException {
        Map<String, MathFunction> available = functionRegistry.getFunctions(resp.getLocale());
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(available.keySet()));
    }

    private void handleArrayCreation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ArrayCreationRequest request = objectMapper.readValue(req.getInputStream(), ArrayCreationRequest.class);
        List<String> xValues = request.getXValues();
        List<String> yValues = request.getYValues();

        if (xValues == null || yValues == null) {
            throw new IllegalArgumentException("Необходимо заполнить оба столбца x и y");
        }
        if (xValues.size() != yValues.size()) {
            throw new IllegalArgumentException("Количество значений x и y должно совпадать");
        }
        if (xValues.size() < 2) {
            throw new IllegalArgumentException("Нужно минимум две точки");
        }
        if (xValues.size() > MAX_POINTS) {
            throw new IllegalArgumentException("Слишком много точек. Максимум: " + MAX_POINTS);
        }

        double[] parsedX = parseValues(xValues, "x");
        double[] parsedY = parseValues(yValues, "y");

        TabulatedFunction function = resolveFactory(request.getFactoryType()).create(parsedX, parsedY);
        respondWithFunction(resp, function, "Таблица точек");
    }

    private void handleFunctionCreation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FunctionCreationRequest request = objectMapper.readValue(req.getInputStream(), FunctionCreationRequest.class);
        if (request.getFunctionName() == null || request.getFunctionName().isBlank()) {
            throw new IllegalArgumentException("Выберите функцию из списка");
        }

        MathFunction source = functionRegistry.getByName(request.getFunctionName());
        double from = parseDouble(request.getFrom(), "Начало интервала должно быть числом");
        double to = parseDouble(request.getTo(), "Конец интервала должен быть числом");
        int count = parseCount(request.getCount());

        if (count > MAX_POINTS) {
            throw new IllegalArgumentException("Слишком большое разбиение. Максимум точек: " + MAX_POINTS);
        }

        TabulatedFunction function = resolveFactory(request.getFactoryType()).create(source, from, to, count);
        respondWithFunction(resp, function, request.getFunctionName());
    }

    private double[] parseValues(List<String> values, String label) {
        double[] result = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            String raw = values.get(i);
            result[i] = parseDouble(raw, "Значение " + label + " №" + (i + 1) + " должно быть числом");
        }
        return result;
    }

    private double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(errorMessage);
        }
    }

    private int parseCount(String rawCount) {
        if (rawCount == null || rawCount.isBlank()) {
            throw new IllegalArgumentException("Введите количество точек");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(rawCount);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Размер таблицы должен быть числом");
        }
        if (parsed < 2) {
            throw new IllegalArgumentException("Размер таблицы не может быть меньше двух");
        }
        return parsed;
    }

    private int parsePositiveInt(String raw, String errorMessage) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < 1) {
                throw new IllegalArgumentException(errorMessage);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new NumberFormatException(errorMessage);
        }
    }

    private void respondWithFunction(HttpServletResponse resp, TabulatedFunction function, String source) throws IOException {
        List<UiPoint> points = new ArrayList<>();
        for (Point point : function) {
            points.add(new UiPoint(point.x, point.y));
        }
        TabulatedFunctionResponse response = new TabulatedFunctionResponse(source, points);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(response));
    }
    private void handleIntegral(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        IntegrationRequest request = objectMapper.readValue(req.getInputStream(), IntegrationRequest.class);
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            throw new IllegalArgumentException("Нужно минимум две точки для интегрирования");
        }

        double from = parseDouble(request.getFrom(), "Нижний предел интегрирования должен быть числом");
        double to = parseDouble(request.getTo(), "Верхний предел интегрирования должен быть числом");
        if (from >= to) {
            throw new IllegalArgumentException("Начало интервала должно быть меньше конца");
        }

        int threads = parsePositiveInt(request.getThreads(), "Количество потоков должно быть положительным");
        TabulatedFunction function = buildFunctionFromPoints(request.getPoints(), request.getFactoryType());

        ParallelIntegralCalculator calculator = new ParallelIntegralCalculator();
        double result = calculator.integrate(function, from, to, threads);

        IntegralResponse response = new IntegralResponse(String.format("Интеграл на [%s; %s]", request.getFrom(), request.getTo()), result);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(response));
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
    private TabulatedFunctionFactory resolveFactory(String factoryType) {
        if (factoryType == null || factoryType.isBlank()) {
            return new ArrayTabulatedFunctionFactory();
        }
        return switch (factoryType.toLowerCase()) {
            case "list", "linked", "linkedlist", "linked-list" -> new LinkedListTabulatedFunctionFactory();
            default -> new ArrayTabulatedFunctionFactory();
        };
    }
}