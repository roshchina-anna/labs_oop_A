package ui;

import functions.*;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class SimpleFunctionRegistry {
    private final Map<String, MathFunction> functions = new TreeMap<>();

    public SimpleFunctionRegistry() {
        functions.put("Единичная функция", new UnitFunction());
        functions.put("Квадратичная функция", new SqrFunction());
        functions.put("Нулевая функция", new ZeroFunction());
        functions.put("Тождественная функция", new IdentityFunction());
    }

    public Map<String, MathFunction> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    public MathFunction getByName(String name) {
        MathFunction mathFunction = functions.get(name);
        if (mathFunction == null) {
            throw new IllegalArgumentException("Неизвестная функция: " + name);
        }
        return mathFunction;
    }
}