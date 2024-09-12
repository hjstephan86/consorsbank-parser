package com.consorsbank.parser;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.Test;

public class BalanceNumberTest {

    @Test
    public void testGetValuePositive() throws Exception {
        String strNumber = "1.243,53+";

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        Number number = decimalFormat.parse(strNumber);
        char sign = strNumber.charAt(strNumber.length() - 1);

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);
        double expectedValue = 1243.53;

        assert (balanceNumber.getValue() == expectedValue);
    }

    @Test
    public void testGetValueNegative() throws Exception {
        String strNumber = "1.243,53-";

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        Number number = decimalFormat.parse(strNumber);
        char sign = strNumber.charAt(strNumber.length() - 1);

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);
        double expectedValue = -1243.53;

        assert (balanceNumber.getValue() == expectedValue);
    }

    @Test
    public void testGetValueLongPositive() throws Exception {
        long value = 123l;
        Number number = value;
        char sign = '+';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == value);
    }

    @Test
    public void testGetValueLongNegative() throws Exception {
        long value = 123l;
        Number number = value;
        char sign = '-';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == -1 * value);
    }

    @Test
    public void testGetValueIntPositive() throws Exception {
        int value = 123;
        Number number = value;
        char sign = '+';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == value);
    }

    @Test
    public void testGetValueIntNegative() throws Exception {
        int value = 123;
        Number number = value;
        char sign = '-';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == -1 * value);
    }

    @Test
    public void testGetValueFloatPositive() throws Exception {
        float value = 12.3f;
        Number number = value;
        char sign = '+';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == value);
    }

    @Test
    public void testGetValueFloatNegative() throws Exception {
        float value = 12.3f;
        Number number = value;
        char sign = '-';

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        assert (balanceNumber.getValue() == -1 * value);
    }
}
