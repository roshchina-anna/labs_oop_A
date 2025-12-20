package io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class FunctionsIOTest {
    private final TabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();

    @Test
    void jsonRoundTrip() throws Exception {
        TabulatedFunction original = factory.create(new double[]{0.0, 1.0, 2.0}, new double[]{0.0, 1.0, 4.0});

        StringWriter writer = new StringWriter();
        FunctionsIO.writeTabulatedFunctionJson(writer, original);

        TabulatedFunction restored = FunctionsIO.readTabulatedFunctionJson(new StringReader(writer.toString()), factory);

        assertEquals(original.getCount(), restored.getCount());
        for (int i = 0; i < original.getCount(); i++) {
            assertEquals(original.getX(i), restored.getX(i));
            assertEquals(original.getY(i), restored.getY(i));
        }
    }

    @Test
    void xmlRoundTrip() throws Exception {
        TabulatedFunction original = factory.create(new double[]{-1.0, 0.0, 1.0}, new double[]{1.0, 0.0, 1.0});

        StringWriter writer = new StringWriter();
        FunctionsIO.writeTabulatedFunctionXml(writer, original);

        TabulatedFunction restored = FunctionsIO.readTabulatedFunctionXml(new StringReader(writer.toString()), factory);

        assertEquals(original.getCount(), restored.getCount());
        for (int i = 0; i < original.getCount(); i++) {
            assertEquals(original.getX(i), restored.getX(i));
            assertEquals(original.getY(i), restored.getY(i));
        }
    }
}