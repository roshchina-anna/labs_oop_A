package ui.swing;

import functions.MathFunction;

public record TabulatedFunctionFactoryParameters(MathFunction source, double from, double to, int count) {
}