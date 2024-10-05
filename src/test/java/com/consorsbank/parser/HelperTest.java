package com.consorsbank.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.junit.Test;
import com.consorsbank.parser.retoure.RetoureHelper;
import com.consorsbank.parser.transfer.BalanceNumber;
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

        trackingId = "1001990069063401019";
        assertFalse(Helper.isHermesTrackingId(trackingId));
    }

    @Test
    public void testIsTrackingIdValid() {
        String trackingId = "2334920001037";
        assertTrue(Helper.isTrackingIdValid(trackingId));

        trackingId = "H1001990069063401019";
        assertTrue(Helper.isTrackingIdValid(trackingId));
    }

    @Test
    public void testPurposeMatches() {
        Transfer t = createTransfer("Amazon", 10, 10, '-', "302-8845287-5188355 Amazon.de SSROA");
        Transfer u = createTransfer("Amazon", 10, 11, '+', "302-8845287-5188355 AMZ Amazon.de 1");

        assertTrue(RetoureHelper.purposeMatches(t, u));

        t.setPurpose("302-8845287-5188357 AMZ Amazon.de 1");
        assertFalse(RetoureHelper.purposeMatches(t, u));

        u.setPurpose("305-1103929-6143535 AMZN Mktp DE 3E");
        t.setPurpose("303-8151108-6247541 AMZN Mktp DE 48");
        assertFalse(RetoureHelper.purposeMatches(t, u));
    }

    private Transfer createTransfer(String name, double value, int day, char sign, String purpose) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.DECEMBER, day, 0, 0, 0);

        Date date = calendar.getTime();
        Number number = value;
        BalanceNumber balanceNumber = new BalanceNumber(number, sign);
        Transfer t = new Transfer(balanceNumber, date);
        t.setName(name);
        t.setPurpose(purpose);

        return t;
    }

    private LinkedHashMap<String, Transfer> getTransferMap(ArrayList<Transfer> transfers) {
        LinkedHashMap<String, Transfer> transferMap = new LinkedHashMap<String, Transfer>();
        for (int i = 0; i < transfers.size(); i++) {
            Transfer transfer = transfers.get(i);
            transfer.setPosition(i + 1);
            transfer.generateHash();
            transferMap.put(transfer.getHash(), transfer);
        }
        return transferMap;
    }

    @Test
    public void testPackageRetoureTransfersNToN() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();

        Transfer t = createTransfer("Amazon", 14.5, 11, '-', "305-1103929-6143535 AMZN Mktp DE IW");
        transfers.add(t);

        Transfer u = createTransfer("Amazon", 21.0, 12, '-', "305-1103929-6143535 AMZN Mktp DE OM");
        transfers.add(u);

        Transfer x = createTransfer("Amazon", 10.5, 14, '+', "305-1103929-6143535 AMZN Mktp DE WU");
        transfers.add(x);

        Transfer y = createTransfer("Amazon", 5.5, 15, '+', "305-1103929-6143535 AMZN Mktp DE LS");
        transfers.add(y);

        LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

        RetoureHelper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));

        RetoureHelper.packageRetoureTransfers(transfers,
                transferMap);
        // However, after packaging we expect a n:n retoure assignment with a chronological ordering
        assertTrue(x.getOutgoingRetoureTransfer().equals(t));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
    }


    @Test
    public void testPackageRetoureTransfersNToM() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();

        Transfer t = createTransfer("Amazon", 14.5, 11, '-', "305-1103929-6143535 AMZN Mktp DE IW");
        transfers.add(t);

        Transfer u = createTransfer("Amazon", 21.0, 12, '-', "305-1103929-6143535 AMZN Mktp DE OM");
        transfers.add(u);

        Transfer x = createTransfer("Amazon", 10.5, 14, '+', "305-1103929-6143535 AMZN Mktp DE WU");
        transfers.add(x);

        Transfer y = createTransfer("Amazon", 5.5, 15, '+', "305-1103929-6143535 AMZN Mktp DE LS");
        transfers.add(y);

        Transfer z = createTransfer("Amazon", 5.5, 16, '+', "305-1103929-6143535 AMZN Mktp DE LS");
        transfers.add(z);

        LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

        RetoureHelper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));

        RetoureHelper.packageRetoureTransfers(transfers,
                transferMap);
        // However, after packaging we expect a n:m retoure assignment with a chronological ordering
        assertTrue(x.getOutgoingRetoureTransfer().equals(t));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));
    }

    @Test
    public void testPackageRetoureTransfersNToMSort() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();

        Transfer t = createTransfer("Amazon", 8.5, 11, '-', "305-1103929-6143535 AMZN Mktp DE IW");
        transfers.add(t);

        Transfer u = createTransfer("Amazon", 21.0, 12, '-', "305-1103929-6143535 AMZN Mktp DE OM");
        transfers.add(u);

        Transfer x = createTransfer("Amazon", 10.5, 13, '+', "305-1103929-6143535 AMZN Mktp DE WU");
        transfers.add(x);

        Transfer y = createTransfer("Amazon", 5.5, 15, '+', "305-1103929-6143535 AMZN Mktp DE LS");
        transfers.add(y);

        Transfer z = createTransfer("Amazon", 5.5, 14, '+', "305-1103929-6143535 AMZN Mktp DE PT");
        transfers.add(z);

        LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

        RetoureHelper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));

        RetoureHelper.packageRetoureTransfers(transfers,
                transferMap);
        // However, after packaging we expect a n:m retoure assignment with a best fit ordering
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(t));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));
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
