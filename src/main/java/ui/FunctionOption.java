package ui;

public class FunctionOption {
    private final String key;
    private final String title;
    private final int priority;

    public FunctionOption(String key, String title, int priority) {
        this.key = key;
        this.title = title;
        this.priority = priority;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }
    public int getPriority() {
        return priority;
    }
}