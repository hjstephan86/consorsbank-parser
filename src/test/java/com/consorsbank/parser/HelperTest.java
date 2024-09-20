package com.consorsbank.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        Transfer transer = Helper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);
        assertTrue(Math.abs(
                transer.getBalanceNumber().getValue() - expectedBalanceValue) < Helper.EPSILON);

        balanceAndDate = "02.01. 8421 02.01. 8.967,42+";
        tokens = new ArrayList<String>();
        tokens.add(transferType);
        tokens.add(balanceAndDate);
        transer = Helper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);

        expectedBalanceValue = 8967.42;
        assertTrue(Math.abs(
                transer.getBalanceNumber().getValue() - expectedBalanceValue) < Helper.EPSILON);

        balanceAndDate = "02.01. 8421 02.01. 0,01-";
        tokens = new ArrayList<String>();
        tokens.add(transferType);
        tokens.add(balanceAndDate);
        transer = Helper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, 2024, 0);

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
        assertTrue(Helper.bankIdValid(bankId));

        bankId = "VISA 58525010 Paderborn";
        assertTrue(Helper.bankIdValid(bankId));

        bankId = "girocard";
        assertFalse(Helper.bankIdValid(bankId));
    }

    @Test
    public void testTrackingIdIsValid() {
        String trackingId = "2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        // Check for at most 20 digits
        trackingId = "JD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        // Check for at least 10 digits
        trackingId = "2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));
    }

    @Test
    public void testPurposeMatches() {
        Transfer t = createTransfer("Amazon", 10, 10, '-', "302-8845287-5188355 Amazon.de SSROA");
        Transfer u = createTransfer("Amazon", 10, 11, '+', "302-8845287-5188355 AMZ Amazon.de 1");

        assertTrue(com.consorsbank.parser.retoure.Helper.purposeMatches(t, u));

        t.setPurpose("302-8845287-5188357 AMZ Amazon.de 1");
        assertFalse(com.consorsbank.parser.retoure.Helper.purposeMatches(t, u));

        u.setPurpose("305-1103929-6143535 AMZN Mktp DE 3E");
        t.setPurpose("303-8151108-6247541 AMZN Mktp DE 48");
        assertFalse(com.consorsbank.parser.retoure.Helper.purposeMatches(t, u));
    }

    private Transfer createTransfer(String name, double value, int day, char sign, String purpose) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        Calendar calendar = Calendar.getInstance();

        calendar.set(2024, Calendar.DECEMBER, day, 0, 0, 0);
        Date date = calendar.getTime();
        Number number = value;
        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);
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

        com.consorsbank.parser.retoure.Helper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));

        com.consorsbank.parser.retoure.Helper.packageRetoureTransfers(transfers, transferMap);
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

        com.consorsbank.parser.retoure.Helper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));

        com.consorsbank.parser.retoure.Helper.packageRetoureTransfers(transfers, transferMap);
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

        com.consorsbank.parser.retoure.Helper.findRetoureTransfers(transfers);
        // Here, we expect a 1:n retoure assignment
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(u));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));

        com.consorsbank.parser.retoure.Helper.packageRetoureTransfers(transfers, transferMap);
        // However, after packaging we expect a n:m retoure assignment with a best fit ordering
        assertTrue(x.getOutgoingRetoureTransfer().equals(u));
        assertTrue(y.getOutgoingRetoureTransfer().equals(t));
        assertTrue(z.getOutgoingRetoureTransfer().equals(u));
    }
}
