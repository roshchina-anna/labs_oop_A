package functions;
import functions.meta.LocalizedName;
import functions.meta.SimpleFunction;

@SimpleFunction(names = {
        @LocalizedName(locale = "ru", value = "Квадратичная функция"),
        @LocalizedName(locale = "en", value = "Square function")
}, priority = 5)
public class SqrFunction implements MathFunction{

    public double apply(double x){
        return Math.pow(x,2);
    }
}
