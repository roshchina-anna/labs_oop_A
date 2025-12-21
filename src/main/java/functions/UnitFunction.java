package functions;
import functions.meta.LocalizedName;
import functions.meta.SimpleFunction;

@SimpleFunction(names = {
        @LocalizedName(locale = "ru", value = "Единичная функция"),
        @LocalizedName(locale = "en", value = "Unit function")
})
public class UnitFunction extends ConstantFunction {
    public UnitFunction() {
        super(1.0);
    }
}
