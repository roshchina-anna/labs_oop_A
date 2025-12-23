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
import java.io.PrintWriter;
import java.util.Iterator;

@WebServlet("/api/tabulated/advanced/*")
@MultipartConfig
public class TabulatedOperationsServlet extends HttpServlet {
    private static final int MAX_POINT_COUNT = 200;
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
        JsonNode pointsNode = body.get("points");
        if (pointsNode == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передайте точки для сохранения");
            return;
        }
        TabulatedFunctionFactory factory = chooseFactory(type);
        try {
            TabulatedFunction function = buildFunction(pointsNode, factory);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            FunctionsIO.serialize(new BufferedOutputStream(buffer), function);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/octet-stream");
            String filename = (name.isBlank() ? "function" : name) + ".bin";
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            resp.getOutputStream().write(buffer.toByteArray());
            resp.flushBuffer();
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
        try {
            TabulatedFunction function = FunctionsIO.deserialize(new BufferedInputStream(filePart.getInputStream()));
            String storage = storageName(function);
            sendJson(resp, HttpServletResponse.SC_OK,
                    objectMapper.writeValueAsString(TabulatedFunctionResponse.from(
                            filePart.getSubmittedFileName() == null ? "Загруженная функция" : filePart.getSubmittedFileName(),
                            storage, function, MAX_POINT_COUNT)));
        } catch (ClassNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный формат файла");
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