package servlets;

import java.util.List;

public class FunctionWithPointsRequest {
    private String name;
    private String expression;
    private List<PointPayload> points;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public List<PointPayload> getPoints() {
        return points;
    }

    public void setPoints(List<PointPayload> points) {
        this.points = points;
    }

    public static class PointPayload {
        private Double xValue;
        private Double yValue;

        public Double getXValue() {
            return xValue;
        }

        public void setXValue(Double xValue) {
            this.xValue = xValue;
        }

        public Double getYValue() {
            return yValue;
        }

        public void setYValue(Double yValue) {
            this.yValue = yValue;
        }
    }
}