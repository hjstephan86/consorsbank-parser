import java.text.DecimalFormat;

public class BalanceNumber {

    private Number number;
    private char sign;
    private DecimalFormat decimalFormat;

    public BalanceNumber(Number number, char sign, DecimalFormat decimalFormat) {
        this.number = number;
        if (sign == '+' || sign == '-') {
            this.sign = sign;
        } else {
            throw new IllegalArgumentException("Sign must be either '+' or '-'");
        }
        this.decimalFormat = decimalFormat;
    }

    /***
     * If the {@link #number} has no decimal places, it is stored as long. Notice,
     * long is casted to double.
     */
    public double getValue() {
        if (number instanceof Double) {
            return sign == '+' ? number.doubleValue() : -1 * number.doubleValue();
        } else if (number instanceof Long) {
            return sign == '+' ? (double) number.longValue() : (double) (-1 * number.longValue());
        } else if (number instanceof Integer) {
            return sign == '+' ? (double) number.intValue() : (double) (-1 * number.intValue());
        } else if (number instanceof Float) {
            return sign == '+' ? (double) number.floatValue() : (double) (-1 * number.floatValue());
        } else {
            return Double.NaN;
        }
    }

    public String toString() {
        return decimalFormat.format(getValue());
    }
}
