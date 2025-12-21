package functions;
import functions.meta.LocalizedName;
import functions.meta.SimpleFunction;

@SimpleFunction(names = {
        @LocalizedName(locale = "ru", value = "Нулевая функция"),
        @LocalizedName(locale = "en", value = "Zero function")
})
public class ZeroFunction extends ConstantFunction {
    public ZeroFunction() {
        super(0.0);
    }
}