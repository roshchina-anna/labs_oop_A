package ui;

import java.util.List;

public class ArrayCreationRequest {
    private List<String> xValues;
    private List<String> yValues;
    private String factoryType;

    public List<String> getXValues() {
        return xValues;
    }

    public void setXValues(List<String> xValues) {
        this.xValues = xValues;
    }

    public List<String> getYValues() {
        return yValues;
    }

    public void setYValues(List<String> yValues) {
        this.yValues = yValues;
    }
    public String getFactoryType() {
        return factoryType;
    }

    public void setFactoryType(String factoryType) {
        this.factoryType = factoryType;
    }
}