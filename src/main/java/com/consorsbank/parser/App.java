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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) throws Exception {
        parseArguments(args);

        File folder = new File(Helper.PATH_TO_PDF_REPORTS);
        File[] listOfPDFReportFiles = folder.listFiles();
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        parseTransfers(listOfPDFReportFiles, transfers);

        Collections.sort(transfers);
        setPosition(transfers);
        findRetourePosititions(transfers);
        List<Transfer> retoureTransfers =
                transfers.stream().filter(transfer -> transfer.getRetourePosition() > 0)
                        .collect(Collectors.toList());

        folder = new File(Helper.PATH_TO_DELIVERY_RECEIPTS);
        File[] listOfReceiptFiles = folder.listFiles();
        HashMap<String, DeliveryReceipt> receiptMap = parseDeliveryReceipts(listOfReceiptFiles);
        ArrayList<DeliveryReceipt> receipts = new ArrayList<DeliveryReceipt>(receiptMap.values());
        Collections.sort(receipts);

        printTransfers(transfers, retoureTransfers);
        printReceipts(receipts);
        assignTrackingIds(transfers, retoureTransfers, receipts);
    }

    private static void parseArguments(String[] args) {
        if (args.length == 3) {
            File pathToPDFReports = new File(args[0]);
            File pathToRetoureLabels = new File(args[1]);
            if (pathToPDFReports.exists() && pathToPDFReports.isDirectory()
                    && pathToRetoureLabels.exists() && pathToRetoureLabels.isDirectory()
                    && args[2].toLowerCase().endsWith(".csv")) {
                Helper.PATH_TO_PDF_REPORTS = args[0];
                Helper.PATH_TO_DELIVERY_RECEIPTS = args[1];
                Helper.PATH_TO_CSV = args[2];
            }
        }
    }

    private static void parseTransfers(File[] listOfFiles, ArrayList<Transfer> transfers)
            throws IOException {
        for (File f : listOfFiles) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                String text = Helper.getPDFReportText(f.getAbsolutePath());
                parseTransfers(text, transfers);
            }
        }
    }

    private static void parseTransfers(String text, ArrayList<Transfer> transfers) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        int positionInMonth = 0;
        ArrayList<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(text, "\n");
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }

        Transfer transfer = null;
        int year = 2000;
        for (int i = 0; i < tokens.size(); i++) {
            String line = tokens.get(i);
            Pattern pattern = Pattern.compile(Helper.PDF_REPORT_REGEX_TRANSFER_TYPES);
            Matcher matcher = pattern.matcher(line);

            if (line.startsWith(Helper.PDF_REPORT_KONTOSTAND_ZUM_IN_TXT) && year == 2000) {
                year = parseYear(year, line);
                continue;
            }
            if (matcher.find()) {
                try {
                    transfer = parseBalanceAndDate(dateFormat, decimalFormat, tokens, year, i);
                    transfers.add(transfer);
                    positionInMonth++;
                    transfer.setPositionInMonth(positionInMonth);
                    parseNameAndBankIdAndPurpose(tokens, transfer, i);
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

    private static void parseNameAndBankIdAndPurpose(ArrayList<String> pdfTokens, Transfer transfer,
            int i) {
        if (Helper.bankIDExists(pdfTokens.get(i + 3))) {
            transfer.setName(pdfTokens.get(i + 2));
            transfer.setBankID(pdfTokens.get(i + 3));

            String purpose = pdfTokens.get(i + 4);
            if (!purpose.startsWith(Helper.PDF_REPORT_INTERIM_KONTOSTAND_ZUM_IN_TXT)) {
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

    private static void findRetourePosititions(ArrayList<Transfer> transfers) {
        outer: for (int i = transfers.size() - 1; i > 0; i--) {
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
                        transfer.setRetourePosition(prevTransfer.getPosition());
                        prevTransfer.setBalanced(true);

                        // Continue with next transfer
                        continue outer;
                    }
                }
            }
        }
    }

    private static HashMap<String, DeliveryReceipt> parseDeliveryReceipts(File[] listOfFiles)
            throws IOException, InterruptedException {
        HashMap<String, DeliveryReceipt> receipts = new HashMap<String, DeliveryReceipt>();
        for (File f : listOfFiles) {
            if (f.isFile() && (f.getName().toLowerCase().endsWith(".jpg")
                    || f.getName().toLowerCase().endsWith(".jpeg")
                    || f.getName().toLowerCase().endsWith(".pdf"))) {
                String text = Helper.getDeliveryReceiptText(f.getAbsolutePath());
                StringTokenizer tokenizer = new StringTokenizer(text, "\n");
                DeliveryReceipt receipt = null;
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    receipt = parseDeliveryReceipt(receipt, token);
                    if (receipt != null
                            && receipt.getRecipient() != null
                            && receipt.getSender() != null
                            && receipt.getDateTime() != null
                            && receipt.getTrackingId() != null) {
                        if (!receipts.containsKey(receipt.getTrackingId())) {
                            receipts.put(receipt.getTrackingId(), receipt);
                            receipt.setFilename(f.getName());
                        }
                    }
                }
            }
        }
        return receipts;
    }

    private static DeliveryReceipt parseDeliveryReceipt(DeliveryReceipt receipt, String token) {
        if (token.startsWith(Helper.DELIVERY_RECEIPT_RECIPIENT_IN_TXT)) {
            receipt = new DeliveryReceipt();
            String recipient = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            receipt.setRecipient(recipient);
        } else if (token.startsWith(Helper.DELIVERY_RECEIPT_SENDER_IN_TXT)) {
            String sender = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            receipt.setSender(sender);
        } else if (token.startsWith(Helper.DELIVERY_RECEIPT_DATE_IN_TXT)) {
            String date = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            receipt.setDate(date);
        } else if (token.startsWith(Helper.DELIVERY_RECEIPT_TIME_IN_TXT)) {
            String time = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            receipt.setTime(time);
        } else if (token.startsWith(Helper.DELIVERY_RECEIPT_TRACKING_ID_IN_TXT)) {
            String trackingId = token.substring(token.indexOf("=") + 1, token.indexOf("}"));
            if (Helper.trackingIDExists(trackingId)) {
                receipt.setTrackingId(trackingId);
            }
        }
        return receipt;
    }

    private static void printTransfers(ArrayList<Transfer> transfers,
            List<Transfer> retoureTransfers) {
        printTransfers(transfers, false);
        System.out.println();
        printTransfers(retoureTransfers, true);
        System.out.println();
    }

    private static void printTransfers(List<Transfer> transfers, boolean color) {
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
            if (color) {
                System.out.println(Helper.CONSOLE_COLOR_CYAN + transfer.getPaddedPosistion()
                        + Helper.CONSOLE_COLOR_RESET + transfer.toPaddedString());
            } else {
                System.out.println(transfer.getPaddedPosistion() + transfer.toPaddedString());
            }
            sum += transfer.getBalanceNumber().getValue();
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));
        System.out.println("Total sum: " + decimalFormat.format(sum));
    }

    private static void printReceipts(ArrayList<DeliveryReceipt> receipts) {
        System.out.println(Helper.padRight("", Helper.EMPTY_COL_WIDTH)
                + Helper.padRight("Sender", Helper.SENDER_COL_WIDTH)
                + Helper.padRight("Recipient", Helper.RECEPIENT_COL_WIDTH)
                + Helper.padRight("Datetime", Helper.DATETIME_COL_WIDTH)
                + Helper.padRight("Tracking Id", Helper.TRACKING_ID_COL_WIDTH)
                + Helper.padRight("Filename", Helper.FILENAME_COL_WIDTH));

        int counter = 1;
        for (DeliveryReceipt receipt : receipts) {
            System.out.println(Helper.CONSOLE_COLOR_YELLOW
                    + Helper.padRight(String.valueOf(counter), Helper.EMPTY_COL_WIDTH)
                    + Helper.CONSOLE_COLOR_RESET + receipt.toPaddedString());
            counter++;
        }
    }

    private static void assignTrackingIds(ArrayList<Transfer> transfers,
            List<Transfer> retoureTransfers, ArrayList<DeliveryReceipt> receipts) {
        printTrackingIdAssignmentDescr();
        Scanner scanner = new Scanner(System.in);
        HashSet<Integer> assignedNumbers = new HashSet<Integer>();
        outer: for (Transfer transfer : retoureTransfers) {
            boolean inputValid = false;
            while (!inputValid) {
                promptForTrackingIdAssignment(transfer);
                String input = scanner.next();
                try {
                    int number = Integer.parseInt(input);
                    DeliveryReceipt retoure = Helper.getDeliveryReceipt(receipts, number);
                    if (retoure != null && !assignedNumbers.contains(number)) {
                        // Assign the tracking id, i.e., the retoure, to the transfer
                        transfer.setDeliveryReceipt(retoure);
                        // An assigned tracking id is not allowed to be assigned twice
                        assignedNumbers.add(number);
                        inputValid = true;

                        if (assignedNumbers.size() == receipts.size()) {
                            break outer;// Skip further assignments, all tracking ids are assigned
                        }
                    }
                } catch (Exception e) {
                    if (input.equals("s")) {
                        continue outer;
                    } else if (input.equals("g")) {
                        break outer;
                    }
                }
            }
        }
        scanner.close();
        exportTransfers(transfers);
    }

    private static void printTrackingIdAssignmentDescr() {
        System.out.println();
        System.out.println(
                "Assign a tracking id to a retoure transfer or enter " + Helper.CONSOLE_COLOR_YELLOW
                        + "g" + Helper.CONSOLE_COLOR_RESET
                        + " for CSV generation or " + Helper.CONSOLE_COLOR_YELLOW + "s"
                        + Helper.CONSOLE_COLOR_RESET + " to skip the retoure transfer.");
    }

    private static void promptForTrackingIdAssignment(Transfer transfer) {
        System.out.print("Enter the " + Helper.CONSOLE_COLOR_YELLOW + "number"
                + Helper.CONSOLE_COLOR_RESET
                + " of the retoure shipment to assign its tracking id to retoure transfer "
                + Helper.CONSOLE_COLOR_CYAN + transfer.getPosition()
                + Helper.CONSOLE_COLOR_RESET
                + ": ");
    }

    private static void exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("Hash;Pos;Date;Balance;Retoure (Pos);BIC;IBAN;Name;Purpose;Tracking Id\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(transfer.toCSVString() + "\n");
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
