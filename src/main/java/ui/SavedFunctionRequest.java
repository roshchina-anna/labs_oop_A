package ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SavedFunctionRequest {
    private final String name;
    private final List<UiPoint> points;

    @JsonCreator
    public SavedFunctionRequest(@JsonProperty("name") String name,
                                @JsonProperty("points") List<UiPoint> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public List<UiPoint> getPoints() {
        return points;
    }
}