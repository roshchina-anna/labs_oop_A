package DTO;

public class Point {
    private Integer functionId;
    private Double xValue;
    private Double yValue;

    public Point() {}

    public Point(Integer functionId, Double xValue, Double yValue) {
        this.functionId = functionId;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    // геттеры и сеттеры
    public Integer getFunctionId() { return functionId; }
    public void setFunctionId(Integer functionId) { this.functionId = functionId; }

    public Double getXValue() { return xValue; }
    public void setXValue(Double xValue) { this.xValue = xValue; }

    public Double getYValue() { return yValue; }
    public void setYValue(Double yValue) { this.yValue = yValue; }

    @Override
    public String toString() {
        return "Point{functionId=" + functionId + ", x=" + xValue + ", y=" + yValue + "}";
    }
}