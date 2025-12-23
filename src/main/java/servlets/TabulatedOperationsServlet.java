package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.InconsistentFunctionsException;
import functions.AbstractTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import io.FunctionsIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import operations.ParallelIntegralCalculator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import operations.TabulatedDifferentialOperator;
import operations.TabulatedFunctionOperationService;
import ui.TabulatedFunctionResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import java.util.Iterator;

@WebServlet("/api/tabulated/advanced/*")
@MultipartConfig
public class TabulatedOperationsServlet extends HttpServlet {
    private static final int MAX_POINT_COUNT = 200;
    private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo();
        if ("/operate".equals(path)) {
            handleOperate(req, resp);
            return;
        }
        if ("/differentiate".equals(path)) {
            handleDifferentiate(req, resp);
            return;
        }
        if ("/serialize".equals(path)) {
            handleSerialize(req, resp);
            return;
        }
        if ("/deserialize".equals(path)) {
            handleDeserialize(req, resp);
            return;
        }
        if ("/apply".equals(path)) {
            handleApply(req, resp);
            return;
        }
        if ("/integrate".equals(path)) {
            handleIntegrate(req, resp);
            return;
        }
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Запрошенный ресурс не найден");
    }

    private void handleOperate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body = parseBody(req, resp);
        if (body == null) {
            return;
        }
        String type = textValue(body, "type");
        String operation = textValue(body, "operation");
        JsonNode leftNode = body.get("left");
        JsonNode rightNode = body.get("right");
        if (leftNode == null || rightNode == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте обе функции для операции");
            return;
        }
        TabulatedFunctionFactory factory = chooseFactory(type);
        try {
            TabulatedFunction left = buildFunction(leftNode, factory);
            TabulatedFunction right = buildFunction(rightNode, factory);
            TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(factory);
            TabulatedFunction result;
            switch (operation.toLowerCase()) {
                case "add":
                    result = service.add(left, right);
                    break;
                case "subtract":
                    result = service.subtract(left, right);
                    break;
                case "multiply":
                    result = service.multiply(left, right);
                    break;
                case "divide":
                    result = service.divide(left, right);
                    break;
                default:
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Неизвестная операция");
                    return;
            }
            sendJson(resp, HttpServletResponse.SC_OK,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from("Результат операции",
                            storageName(type), result, MAX_POINT_COUNT)));
        } catch (IllegalArgumentException | InconsistentFunctionsException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDifferentiate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body = parseBody(req, resp);
        if (body == null) {
            return;
        }
        String type = textValue(body, "type");
        JsonNode sourceNode = body.get("source");
        if (sourceNode == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите функцию для дифференцирования");
            return;
        }
        TabulatedFunctionFactory factory = chooseFactory(type);
        try {
            TabulatedFunction source = buildFunction(sourceNode, factory);
            TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(factory);
            TabulatedFunction result = operator.derive(source);
            sendJson(resp, HttpServletResponse.SC_OK,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from("Производная",
                            storageName(type), result, MAX_POINT_COUNT)));
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleSerialize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body = parseBody(req, resp);
        if (body == null) {
            return;
        }
        String name = textValue(body, "name");
        String type = textValue(body, "type");
        String format = textValue(body, "format").isBlank() ? "bin" : textValue(body, "format");
        JsonNode pointsNode = body.get("points");
        if (pointsNode == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте точки для сохранения");
            return;
        }
        TabulatedFunctionFactory factory = chooseFactory(type);
        try {
            TabulatedFunction function = buildFunction(pointsNode, factory);
            String baseName = name.isBlank() ? "function" : name;
            switch (format.toLowerCase()) {
                case "json" -> writeJsonFunction(resp, function, baseName);
                case "xml" -> writeXmlFunction(resp, function, baseName);
                default -> writeBinaryFunction(resp, function, baseName);
            }
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeserialize(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Загрузите файл для чтения функции");
            return;
        }
        String typeParam = req.getParameter("type");
        String format = req.getParameter("format");
        if (format == null || format.isBlank()) {
            format = detectFormat(filePart.getSubmittedFileName());
        }
        try {
            TabulatedFunction function;
            if ("json".equalsIgnoreCase(format)) {
                function = FunctionsIO.readTabulatedFunctionJson(
                        new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8),
                        chooseFactory(typeParam));
            } else if ("xml".equalsIgnoreCase(format)) {
                function = FunctionsIO.readTabulatedFunctionXml(
                        new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8),
                        chooseFactory(typeParam));
            } else {
                function = FunctionsIO.deserialize(new BufferedInputStream(filePart.getInputStream()));
            }
            String storage = storageName(function);
            sendJson(resp, HttpServletResponse.SC_OK,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from(
                            filePart.getSubmittedFileName() == null ? "Загруженная функция" : filePart.getSubmittedFileName(),
                            storage, function, MAX_POINT_COUNT)));
        } catch (ClassNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный формат файла");
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private JsonNode parseBody(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            return objectMapper.readTree(req.getInputStream());
        } catch (JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный JSON");
            return null;
        }
    }
    private void handleApply(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body = parseBody(req, resp);
        if (body == null) {
            return;
        }
        JsonNode xNode = body.get("x");
        JsonNode functionNode = body.get("function");
        if (functionNode == null || xNode == null || !xNode.isNumber()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте функцию и значение x");
            return;
        }
        TabulatedFunctionFactory factory = chooseFactory(textValue(body, "type"));
        try {
            TabulatedFunction function = buildFunction(functionNode, factory);
            double value = function.apply(xNode.asDouble());
            sendJson(resp, HttpServletResponse.SC_OK, "{\"value\":" + value + "}");
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleIntegrate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonNode body = parseBody(req, resp);
        if (body == null) {
            return;
        }
        JsonNode functionNode = body.get("function");
        if (functionNode == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте функцию для интегрирования");
            return;
        }
        int threads = body.has("threads") && body.get("threads").canConvertToInt()
                ? body.get("threads").asInt() : 1;
        threads = Math.max(1, Math.min(MAX_THREADS, threads));
        TabulatedFunctionFactory factory = chooseFactory(textValue(body, "type"));
        try {
            TabulatedFunction function = buildFunction(functionNode, factory);
            ParallelIntegralCalculator calculator = new ParallelIntegralCalculator();
            double result = calculator.integrate(function, function.leftBound(), function.rightBound(), threads);
            sendJson(resp, HttpServletResponse.SC_OK,
                    "{\"value\":" + result + ",\"threadsUsed\":" + threads + "}");
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
    private TabulatedFunction buildFunction(JsonNode node, TabulatedFunctionFactory factory) {
        JsonNode pointsNode = node.has("points") ? node.get("points") : node;
        if (pointsNode == null || !pointsNode.isArray()) {
            throw new IllegalArgumentException("Точки функции должны быть массивом");
        }
        int count = pointsNode.size();
        if (count == 0) {
            throw new IllegalArgumentException("Нужно добавить хотя бы одну точку");
        }
        double[] xValues = new double[count];
        double[] yValues = new double[count];
        Iterator<JsonNode> iterator = pointsNode.iterator();
        for (int i = 0; i < count; i++) {
            JsonNode point = iterator.next();
            if (point.get("x") == null || point.get("y") == null || !point.get("x").isNumber() || !point.get("y").isNumber()) {
                throw new IllegalArgumentException("Каждая точка должна содержать числовые x и y");
            }
            xValues[i] = point.get("x").asDouble();
            yValues[i] = point.get("y").asDouble();
        }
        if (count > 1) {
            AbstractTabulatedFunction.checkSorted(xValues);
        }
        return factory.create(xValues, yValues);
    }
    private void writeBinaryFunction(HttpServletResponse resp, TabulatedFunction function, String baseName) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FunctionsIO.serialize(new BufferedOutputStream(buffer), function);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".bin\"");
        resp.getOutputStream().write(buffer.toByteArray());
        resp.flushBuffer();
    }

    private void writeJsonFunction(HttpServletResponse resp, TabulatedFunction function, String baseName) throws IOException {
        StringWriter writer = new StringWriter();
        FunctionsIO.writeTabulatedFunctionJson(writer, function);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".json\"");
        resp.getWriter().write(writer.toString());
        resp.flushBuffer();
    }

    private void writeXmlFunction(HttpServletResponse resp, TabulatedFunction function, String baseName) throws IOException {
        StringWriter writer = new StringWriter();
        FunctionsIO.writeTabulatedFunctionXml(writer, function);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".xml\"");
        resp.getWriter().write(writer.toString());
        resp.flushBuffer();
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

    private String storageName(TabulatedFunction function) {
        if (function instanceof LinkedListTabulatedFunction) {
            return "Связный список";
        }
        return "Массив";
    }
    private String detectFormat(String filename) {
        if (filename == null) {
            return "bin";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".json")) {
            return "json";
        }
        if (lower.endsWith(".xml")) {
            return "xml";
        }
        return "bin";
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual()) {
            return "";
        }
        return value.asText("");
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
}