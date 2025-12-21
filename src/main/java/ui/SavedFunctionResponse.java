package ui;

import java.util.List;

public class SavedFunctionResponse {
    private final Integer id;
    private final String name;
    private final List<UiPoint> points;

    public SavedFunctionResponse(Integer id, String name, List<UiPoint> points) {
        this.id = id;
        this.name = name;
        this.points = points;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<UiPoint> getPoints() {
        return points;
    }
}