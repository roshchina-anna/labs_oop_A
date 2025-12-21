package ui;

import java.util.List;

public record SavedFunctionResponse(Integer id, String name, List<UiPoint> points) {
}