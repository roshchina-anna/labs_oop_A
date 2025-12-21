package ui;

import java.util.List;

public class SavedFunctionRequest {
    private String name;
    private List<UiPoint> points;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UiPoint> getPoints() {
        return points;
    }

    public void setPoints(List<UiPoint> points) {
        this.points = points;
    }
}