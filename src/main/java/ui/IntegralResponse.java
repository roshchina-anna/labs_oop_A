package ui;

public class IntegralResponse {
    private final String source;
    private final double result;

    public IntegralResponse(String source, double result) {
        this.source = source;
        this.result = result;
    }

    public String getSource() {
        return source;
    }

    public double getResult() {
        return result;
    }
}