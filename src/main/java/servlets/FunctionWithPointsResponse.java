package servlets;

import java.util.List;

public class FunctionWithPointsResponse {
    private Integer id;
    private String name;
    private String expression;
    private Integer userId;
    private List<FunctionWithPointsRequest.PointPayload> points;

    public FunctionWithPointsResponse(Integer id, String name, String expression, Integer userId, List<FunctionWithPointsRequest.PointPayload> points) {
        this.id = id;
        this.name = name;
        this.expression = expression;
        this.userId = userId;
        this.points = points;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public Integer getUserId() {
        return userId;
    }

    public List<FunctionWithPointsRequest.PointPayload> getPoints() {
        return points;
    }
}