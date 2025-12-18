package DTO;

public class Function {
    private Integer id;
    private String name;
    private String expression;
    private Integer userId;

    public Function() {}

    public Function(String name, String expression, Integer userId) {
        this.name = name;
        this.expression = expression;
        this.userId = userId;
    }

    // геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Function{id=" + id + ", name='" + name + "', expression='" + expression + "', userId=" + userId + "}";
    }
}