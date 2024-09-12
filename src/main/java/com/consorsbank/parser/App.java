package com.consorsbank.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    public static void main(String[] args) throws Exception {
        parseArguments(args);

        File folderPDF = new File(Helper.PATH_TO_PDF_REPORTS);
        File[] listOfPDFFiles = folderPDF.listFiles();
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        readTransfers(listOfPDFFiles, transfers);

        Collections.sort(transfers);
        setPosition(transfers);
        findRetoure(transfers);

        File folderJPEG = new File(Helper.PATH_TO_RETOURE_LABELS);
        File[] listOfJPEGFiles = folderJPEG.listFiles();
        HashMap<String, Retoure> labels = parseRetoureLabels(listOfJPEGFiles);

        printTransfers(transfers);
        exportTransfers(transfers);
    }

    private static void parseArguments(String[] args) {
        if (args.length == 3) {
            File pathToPDFReports = new File(args[0]);
            File pathToRetoureLabels = new File(args[1]);
            if (pathToPDFReports.exists() && pathToPDFReports.isDirectory()
                    && pathToRetoureLabels.exists() && pathToRetoureLabels.isDirectory()
                    && args[2].toLowerCase().endsWith(".csv")) {
                Helper.PATH_TO_PDF_REPORTS = args[0];
                Helper.PATH_TO_RETOURE_LABELS = args[1];
                Helper.PATH_TO_CSV = args[2];
            }
        }
    }

    private static HashMap<String, Retoure> parseRetoureLabels(File[] listOfJPEGFiles)
            throws IOException, InterruptedException {
        HashMap<String, Retoure> labels = new HashMap<String, Retoure>();
        for (File f : listOfJPEGFiles) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".jpg")) {
                String jpegText = Helper.getJPEGText(f.getAbsolutePath());
                StringTokenizer jpegTokenizer = new StringTokenizer(jpegText, "\n");
                Retoure label = null;
                while (jpegTokenizer.hasMoreTokens()) {
                    String token = jpegTokenizer.nextToken();
                    label = parseRetourelabel(label, token);
                    if (label != null
                            && label.getRecipient() != null
                            && label.getSender() != null
                            && label.getDateTime() != null
                            && label.getTrackingId() != null) {
                        if (!labels.containsKey(label.getTrackingId())) {
                            labels.put(label.getTrackingId(), label);
                        }
                    }
                }
            }
        }
        return labels;
    }

    private static Retoure parseRetourelabel(Retoure label, String token) {
        if (token.startsWith(":recipient_name: [{value=")) {
            label = new Retoure();
            String recipient = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            label.setRecipient(recipient);
        }
        if (token.startsWith(":sender_name: [{value=")) {
            String sender = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            label.setSender(sender);
        }
        if (token.startsWith(":shipment_date: [{value=")) {
            String date = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            label.setDate(date);
        }
        if (token.startsWith(":shipment_time: [{value=")) {
            String time = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            label.setTime(time);
        }
        if (token.startsWith(":tracking_number: [{value=")) {
            String trackingId = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            if (Helper.trackingIDExists(trackingId)) {
                label.setTrackingId(trackingId);
            }
        }
        return label;
    }

    private static void readTransfers(File[] listOfFiles, ArrayList<Transfer> transfers)
            throws IOException {
        for (File f : listOfFiles) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                String pdfText = Helper.getPDFText(f.getAbsolutePath());
                readTransfers(pdfText, transfers);
            }
        }
    }

    private static void readTransfers(String pdfText, ArrayList<Transfer> transfers) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        ArrayList<String> pdfTokens = new ArrayList<String>();
        StringTokenizer pdfTokenizer = new StringTokenizer(pdfText, "\n");
        while (pdfTokenizer.hasMoreTokens()) {
            pdfTokens.add(pdfTokenizer.nextToken());
        }

        Transfer transfer = null;
        int year = 2000;
        for (int i = 0; i < pdfTokens.size(); i++) {
            String line = pdfTokens.get(i);
            Pattern pattern = Pattern.compile(Helper.PDF_REGEX_TRANSFER_TYPES);
            Matcher matcher = pattern.matcher(line);

            if (line.startsWith(Helper.PDF_KONTOSTAND_ZUM_IN_TXT) && year == 2000) {
                year = parseYear(year, line);
                continue;
            }
            if (matcher.find()) {
                try {
                    transfer = parseBalanceAndDate(dateFormat, decimalFormat, pdfTokens, year, i);
                    transfers.add(transfer);
                    parseNameAndBankIDAndPurpose(pdfTokens, transfer, i);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int parseYear(int year, String line) {
        String[] strLine = line.split(" ");
        String[] strDate = strLine[2].split("\\.");
        year += Integer.parseInt(strDate[2]);
        return year;
    }

    private static Transfer parseBalanceAndDate(SimpleDateFormat dateFormat,
            DecimalFormat decimalFormat, ArrayList<String> pdfTokens, int year, int i)
            throws ParseException {
        String[] splittedLine = pdfTokens.get(i + 1).split(" ");
        Date date = dateFormat.parse(splittedLine[0] + year);
        Number number = decimalFormat.parse(splittedLine[3]);

        char sign = splittedLine[3].charAt(splittedLine[3].length() - 1);
        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        Transfer transfer = new Transfer(balanceNumber, date);
        return transfer;
    }

    private static void parseNameAndBankIDAndPurpose(ArrayList<String> pdfTokens, Transfer transfer,
            int i) {
        if (Helper.bankIDExists(pdfTokens.get(i + 3))) {
            transfer.setName(pdfTokens.get(i + 2));
            transfer.setBankID(pdfTokens.get(i + 3));

            String purpose = pdfTokens.get(i + 4);
            if (!purpose.startsWith(Helper.PDF_INTERIM_KONTOSTAND_ZUM_IN_TXT)) {
                transfer.setPurpose(purpose);
            }
        } else {
            String purpose = pdfTokens.get(i + 2) + " " + pdfTokens.get(i + 3);
            transfer.setPurpose(purpose);
        }
    }

    private static void setPosition(ArrayList<Transfer> transfers) {
        for (int i = 0; i < transfers.size(); i++) {
            Transfer transfer = transfers.get(i);
            transfer.setPosition(i + 1);
        }
    }

    private static void findRetoure(ArrayList<Transfer> transfers) {
        outerLoop: for (int i = transfers.size() - 1; i > 0; i--) {
            Transfer transfer = transfers.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Transfer prevTransfer = transfers.get(j);
                long daysBetween = ChronoUnit.DAYS.between(prevTransfer.getLocalDate(),
                        transfer.getLocalDate());
                if (daysBetween <= Helper.RETOURE_LIMIT_DAYS && !prevTransfer.isBalanced()) {
                    double balanceValue = transfer.getBalanceNumber().getValue();
                    double prevBalanceValue = prevTransfer.getBalanceNumber().getValue();

                    double epsilon = 1e-9;
                    if (balanceValue > 0 && prevBalanceValue < 0
                            && Math.abs(balanceValue - Math.abs(prevBalanceValue)) < epsilon
                            && transfer.getName().equals(prevTransfer.getName())) {
                        transfer.setRetoure(prevTransfer.getPosition());
                        prevTransfer.setBalanced(true);

                        // Continue with next transfer
                        continue outerLoop;
                    }
                }
            }
        }
    }

    private static void printTransfers(ArrayList<Transfer> transfers) {
        double sum = 0;
        System.out.println(
                Helper.padRight("Pos", Helper.POS_COL_WIDTH)
                        + Helper.padRight("Date", Helper.DATE_COL_WIDTH)
                        + Helper.padRight("Balance", Helper.BALANCE_COL_WIDTH)
                        + Helper.padRight("Retoure (Pos)", Helper.RETOURE_COL_WIDTH)
                        + Helper.padRight("BIC", Helper.BIC_COL_WIDTH)
                        + Helper.padRight("IBAN", Helper.IBAN_COL_WIDTH)
                        + Helper.padRight("Name", Helper.NAME_COL_WIDTH)
                        + "Purpose");

        for (Transfer transfer : transfers) {
            System.out.println(transfer.toPaddedString());
            sum += transfer.getBalanceNumber().getValue();
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));
        System.out.println("Total sum: " + decimalFormat.format(sum));
    }

    private static void exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();

        int counter = 1;
        stringBuilder.append("Pos;Date;Balance;Retoure (Pos);BIC;IBAN;Name;Purpose\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(String.valueOf(counter++) + ";" + transfer.toCSVString() + "\n");
        }

        try {
            Helper.PATH_TO_CSV = Helper.PATH_TO_CSV.replace("%DATETIME%",
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                            .format(Calendar.getInstance().getTime()));
            FileWriter writer = new FileWriter(Helper.PATH_TO_CSV);
            writer.write(stringBuilder.toString());
            writer.close();
            System.out.println("Successfully exported transfers to " + Helper.PATH_TO_CSV + ".");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to " + Helper.PATH_TO_CSV + ".");
            e.printStackTrace();
        }
    }
}
