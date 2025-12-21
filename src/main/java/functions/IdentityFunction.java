package functions;
import functions.meta.LocalizedName;
import functions.meta.SimpleFunction;

@SimpleFunction(names = {
        @LocalizedName(locale = "ru", value = "Тождественная функция"),
        @LocalizedName(locale = "en", value = "Identity function")
}, priority = 10)
public class IdentityFunction implements MathFunction {

    @Override
    public double apply(double x) {
        return x;
    }
}
