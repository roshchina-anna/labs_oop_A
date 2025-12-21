package ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class IntegrationRequest {
    private final List<UiPoint> points;
    private final String from;
    private final String to;
    private final String threads;
    private final String factoryType;

    @JsonCreator
    public IntegrationRequest(@JsonProperty("points") List<UiPoint> points,
                              @JsonProperty("from") String from,
                              @JsonProperty("to") String to,
                              @JsonProperty("threads") String threads,
                              @JsonProperty("factoryType") String factoryType) {
        this.points = points;
        this.from = from;
        this.to = to;
        this.threads = threads;
        this.factoryType = factoryType;
    }

    public List<UiPoint> getPoints() {
        return points;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getThreads() {
        return threads;
    }

    public String getFactoryType() {
        return factoryType;
    }
}