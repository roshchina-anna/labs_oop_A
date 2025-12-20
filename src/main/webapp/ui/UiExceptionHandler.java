package ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class UiExceptionHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int SC_UNPROCESSABLE_ENTITY = 422;

    public void handle(Exception exception, HttpServletResponse response) throws IOException {
        int status = defineStatus(exception);
        String message = exception.getMessage() == null ? "Ошибка обработки данных" : exception.getMessage();

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("error", message);
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private int defineStatus(Exception exception) {
        if (exception instanceof NumberFormatException) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
        if (exception instanceof IllegalArgumentException) {
            return SC_UNPROCESSABLE_ENTITY;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}