package ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExportRequest {
    private final String format;
    private final String name;
    private final String factoryType;
    private final List<UiPoint> points;

    @JsonCreator
    public ExportRequest(@JsonProperty("format") String format,
                         @JsonProperty("name") String name,
                         @JsonProperty("factoryType") String factoryType,
                         @JsonProperty("points") List<UiPoint> points) {
        this.format = format;
        this.name = name;
        this.factoryType = factoryType;
        this.points = points;
    }

    public String getFormat() {
        return format;
    }

    public String getName() {
        return name;
    }

    public String getFactoryType() {
        return factoryType;
    }

    public List<UiPoint> getPoints() {
        return points;
    }
}