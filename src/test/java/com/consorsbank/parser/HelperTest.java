package com.consorsbank.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import org.junit.Test;
import com.consorsbank.parser.transfer.Transfer;
import com.consorsbank.parser.transfer.TransferHelper;

public class HelperTest {

    @Test
    public void testParseBalanceAndDate() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        String transferType = "EURO-UEBERW.";
        String balanceAndDate = "02.01. 8421 02.01. 5.393,99-";
        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(transferType);
        tokens.add(balanceAndDate);

        double expectedBalanceValue = -5393.99;
        Transfer transer =
                TransferHelper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);
        assertTrue(Math.abs(
                transer.getBalanceNumber().getValue() - expectedBalanceValue) < Helper.EPSILON);

        balanceAndDate = "02.01. 8421 02.01. 8.967,42+";
        tokens = new ArrayList<String>();
        tokens.add(transferType);
        tokens.add(balanceAndDate);
        transer = TransferHelper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);

        expectedBalanceValue = 8967.42;
        assertTrue(Math.abs(
                transer.getBalanceNumber().getValue() - expectedBalanceValue) < Helper.EPSILON);

        balanceAndDate = "02.01. 8421 02.01. 0,01-";
        tokens = new ArrayList<String>();
        tokens.add(transferType);
        tokens.add(balanceAndDate);
        transer = TransferHelper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);

        expectedBalanceValue = -0.01;
        assertTrue(Math.abs(
                transer.getBalanceNumber().getValue() - expectedBalanceValue) < Helper.EPSILON);

    }

    @Test
    public void testGetHash() {
        String expectedHash = "14ab2924adb4c2f0ce9d37b0afa98a9d";
        String input = "My Input";

        String hash = Helper.getHash(input);

        assert (hash.equals(expectedHash));
    }

    @Test
    public void testBankIdValid() {
        String bankId = "<WELADED1WDB> DE33478535200003845849";
        assertTrue(TransferHelper.bankIdValid(bankId));

        bankId = "VISA 58525010 Paderborn";
        assertTrue(TransferHelper.bankIdValid(bankId));

        bankId = "girocard";
        assertFalse(TransferHelper.bankIdValid(bankId));
    }

    @Test
    public void testIsDHLTrackingId() {
        String trackingId = "2334920001037";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "JJD2334920001037";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "JD2334920001037";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        // Check for at most 20 digits
        trackingId = "JD23349200010375785260";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        trackingId = "JJD23349200010375785260";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "JJD233492000103757852602";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        // Check for at least 10 digits
        trackingId = "2334920001";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "233492000";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        trackingId = "JD233492000";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        trackingId = "JD2334920001";
        assertTrue(Helper.isDHLTrackingId(trackingId));

        trackingId = "JJD233492000";
        assertFalse(Helper.isDHLTrackingId(trackingId));

        trackingId = "JJD2334920001";
        assertTrue(Helper.isDHLTrackingId(trackingId));
    }

    @Test
    public void testIsHermesTrackingId() {
        String trackingId = "H1001990069063401019";
        assertTrue(Helper.isHermesTrackingId(trackingId));

        trackingId = "H100199006906340101";
        assertFalse(Helper.isHermesTrackingId(trackingId));

        trackingId = "H10019900690634010199";
        assertFalse(Helper.isHermesTrackingId(trackingId));

        trackingId = "1001990069063401019";
        assertFalse(Helper.isHermesTrackingId(trackingId));
    }

    @Test
    public void testIsUPSTrackingId() {
        String trackingId = "E4016129636432";
        assertTrue(Helper.isUPSTrackingId(trackingId));

        trackingId = "E401612963643";
        assertFalse(Helper.isUPSTrackingId(trackingId));

        trackingId = "E40161296364322";
        assertFalse(Helper.isUPSTrackingId(trackingId));

        trackingId = "4016129636432";
        assertFalse(Helper.isUPSTrackingId(trackingId));
    }

    @Test
    public void testIsTrackingIdValid() {
        String trackingId = "2334920001037";
        assertTrue(Helper.isTrackingIdValid(trackingId));

        trackingId = "H1001990069063401019";
        assertTrue(Helper.isTrackingIdValid(trackingId));

        trackingId = "E4016129636432";
        assertTrue(Helper.isTrackingIdValid(trackingId));
    }

    @Test
    public void testGetFileChecksum() {
        String algorithm = Helper.SHA_ALGORITHM; // You can use MD5, SHA-1, SHA-256, etc.
        String oldFilePath =
                "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/Scanned_20240827-1312.jpg";
        String newFilePath =
                "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/Scanned_20240827-1312-new.jpg";
        String expectedHash = "1ff938c5e589b2f10038ac3acf719ef88e3872f3f243cac0a92c1b26500dcfdd";

        String fileHash = Helper.getFileHash(algorithm, oldFilePath);
        assertTrue(expectedHash.equals(fileHash));

        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);

        oldFile.renameTo(newFile);
        assertTrue(expectedHash.equals(fileHash));

        newFile.renameTo(oldFile);
    }
}
