package com.consorsbank.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

    public static void main(String[] args) throws Exception {
        parseArgs(args);

        File folder = new File(Helper.PATH_TO_PDF_REPORTS);
        File[] listOfPDFReportFiles = folder.listFiles();
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        parseTransfers(listOfPDFReportFiles, transfers);

        Collections.sort(transfers);
        LinkedHashMap<String, Transfer> transferMap = setPosition(transfers);
        com.consorsbank.parser.retoure.Helper.findRetoureTransfers(transfers);
        com.consorsbank.parser.retoure.Helper.packageRetoureTransfers(transfers, transferMap);

        HashSet<String> existingTrackingIds = new HashSet<String>();
        parseForExsistingTrackingIds(transferMap, existingTrackingIds);
        List<Transfer> retoureTransfers =
                transfers.stream()
                        .filter(transfer -> transfer.getOutgoingRetoureTransfer() != null
                                && transfer.getExistingTrackingId() == null)
                        .collect(Collectors.toList());

        ArrayList<DeliveryReceipt> existingReceipts = parseDeliveryReceipts();
        folder = new File(Helper.PATH_TO_DELIVERY_RECEIPTS);
        File[] listOfReceiptFiles = folder.listFiles();
        listOfReceiptFiles = removeExistingDeliveryReceipts(listOfReceiptFiles, existingReceipts);
        HashMap<String, DeliveryReceipt> receiptMap = parseDeliveryReceipts(listOfReceiptFiles);
        ArrayList<DeliveryReceipt> receipts = new ArrayList<DeliveryReceipt>(receiptMap.values());
        Collections.sort(receipts);

        printTransfers(transfers, retoureTransfers);
        List<DeliveryReceipt> allReceipts =
                Stream.concat(receipts.stream(), existingReceipts.stream())
                        .collect(Collectors.toList());
        printReceipts(allReceipts, existingTrackingIds);
        assignTrackingIds(transfers, retoureTransfers, allReceipts);
    }

    private static void parseArgs(String[] args) {
        switch (args.length) {
            case 2:
                parsePDFsAndReceiptsPathsArgs(args);
                break;
            case 3:
                parsePDFsAndReceiptsPathsArgs(args);
                parseArgTransferExport(args);
                break;
            case 4:
                parsePDFsAndReceiptsPathsArgs(args);
                parseArgTransferExport(args);
                parseArgTransferImport(args);
                break;
            case 5:
                parsePDFsAndReceiptsPathsArgs(args);
                parseArgTransferExport(args);
                parseArgTransferImport(args);
                parseArgReceiptsExport(args);
                break;
            case 6:
                parsePDFsAndReceiptsPathsArgs(args);
                parseArgTransferExport(args);
                parseArgTransferImport(args);
                parseArgReceiptsExport(args);
                parseArgReceiptsImport(args);
                break;
            default:
                break;
        }
    }

    private static void parsePDFsAndReceiptsPathsArgs(String[] args) {
        File pathToPDFReports = new File(args[0]);
        File pathToDeliveryReceipts = new File(args[1]);
        if (pathToPDFReports.exists() && pathToPDFReports.isDirectory()
                && pathToDeliveryReceipts.exists() && pathToDeliveryReceipts.isDirectory()) {
            Helper.PATH_TO_PDF_REPORTS = args[0];
            Helper.PATH_TO_DELIVERY_RECEIPTS = args[1];
        }
    }

    private static void parseArgTransferExport(String[] args) {
        File pathToTransfersExport = new File(args[2]);
        if (pathToTransfersExport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_TRANSFERS_EXPORT = pathToTransfersExport.getAbsolutePath();
        }
    }

    private static void parseArgTransferImport(String[] args) {
        File pathToTransfersImport = new File(args[3]);
        if (pathToTransfersImport.exists() && pathToTransfersImport.isFile()
                && pathToTransfersImport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_TRANSFERS_IMPORT = pathToTransfersImport.getAbsolutePath();
        }
    }

    private static void parseArgReceiptsExport(String[] args) {
        File pathToDeliveryReceiptsExport = new File(args[4]);
        if (pathToDeliveryReceiptsExport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_DELIVERY_RECEIPTS_EXPORT =
                    pathToDeliveryReceiptsExport.getAbsolutePath();
        }
    }

    private static void parseArgReceiptsImport(String[] args) {
        File pathToDeliveryReceiptsImport = new File(args[5]);
        if (pathToDeliveryReceiptsImport.exists() && pathToDeliveryReceiptsImport.isFile()
                && pathToDeliveryReceiptsImport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_DELIVERY_RECEIPTS_IMPORT =
                    pathToDeliveryReceiptsImport.getAbsolutePath();
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
        DecimalFormat decimalFormat = new DecimalFormat();
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
                    transfer =
                            Helper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, year, i);
                    transfers.add(transfer);
                    positionInMonth++;
                    transfer.setPositionInMonth(positionInMonth);
                    Helper.parseNameAndBankIdAndPurpose(tokens, transfer, i);
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

    private static LinkedHashMap<String, Transfer> setPosition(ArrayList<Transfer> transfers) {
        LinkedHashMap<String, Transfer> transferMap = new LinkedHashMap<String, Transfer>();
        for (int i = 0; i < transfers.size(); i++) {
            Transfer transfer = transfers.get(i);
            transfer.setPosition(i + 1);
            transfer.generateHash();
            transferMap.put(transfer.getHash(), transfer);
        }
        return transferMap;
    }

    private static void parseForExsistingTrackingIds(LinkedHashMap<String, Transfer> transferMap,
            HashSet<String> existingTrackingIds) {
        try (BufferedReader br =
                new BufferedReader(new FileReader(Helper.PATH_TO_TRANSFERS_IMPORT))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] transferArr = line.split(";");
                String hashFromCSV = transferArr[0];
                if (transferArr.length == 10) {
                    String existingTrackingId = transferArr[9];
                    if (Helper.trackingIdIsValid(existingTrackingId)
                            && transferMap.containsKey(hashFromCSV)) {
                        Transfer transfer = transferMap.get(hashFromCSV);
                        transfer.setExistingTrackingId(existingTrackingId);
                        existingTrackingIds.add(existingTrackingId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File[] removeExistingDeliveryReceipts(File[] listOfReceiptFiles,
            ArrayList<DeliveryReceipt> existingReceipts) {
        ArrayList<File> listOfRemainingReceiptFiles = new ArrayList<File>();
        HashSet<String> existingDeliveryReceiptFilenames = new HashSet<String>();
        for (DeliveryReceipt receipt : existingReceipts) {
            existingDeliveryReceiptFilenames.add(receipt.getFilename());
        }
        for (int i = 0; i < listOfReceiptFiles.length; i++) {
            if (listOfReceiptFiles[i].isFile() && !existingDeliveryReceiptFilenames
                    .contains(listOfReceiptFiles[i].getName())) {
                listOfRemainingReceiptFiles.add(listOfReceiptFiles[i]);
            }
        }
        return listOfRemainingReceiptFiles.stream().toArray(File[]::new);
    }

    private static HashMap<String, DeliveryReceipt> parseDeliveryReceipts(File[] listOfFiles)
            throws IOException, InterruptedException {
        HashMap<String, DeliveryReceipt> receiptMap = new HashMap<String, DeliveryReceipt>();
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
                        if (!receiptMap.containsKey(receipt.getTrackingId())) {
                            receiptMap.put(receipt.getTrackingId(), receipt);
                            receipt.setFilename(f.getName());
                        }
                    }
                }
            }
        }
        return receiptMap;
    }

    private static ArrayList<DeliveryReceipt> parseDeliveryReceipts() {
        ArrayList<DeliveryReceipt> receipts = new ArrayList<DeliveryReceipt>();
        try (BufferedReader br =
                new BufferedReader(new FileReader(Helper.PATH_TO_DELIVERY_RECEIPTS_IMPORT))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] receiptArr = line.split(";");
                String trackingId = receiptArr[3];
                if (receiptArr.length == 5 && Helper.trackingIdIsValid(trackingId)) {
                    DeliveryReceipt receipt = new DeliveryReceipt();
                    receipt.setSender(receiptArr[0]);
                    receipt.setRecipient(receiptArr[1]);
                    receipt.setDateTime(receiptArr[2]);
                    receipt.setTrackingId(trackingId);
                    receipt.setFilename(receiptArr[4]);
                    receipts.add(receipt);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            if (Helper.trackingIdIsValid(trackingId)) {
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

    private static void printReceipts(List<DeliveryReceipt> receipts,
            HashSet<String> existingTrackingIds) {
        System.out.println(Helper.padRight("", Helper.EMPTY_COL_WIDTH)
                + Helper.padRight("Sender", Helper.SENDER_COL_WIDTH)
                + Helper.padRight("Recipient", Helper.RECEPIENT_COL_WIDTH)
                + Helper.padRight("Datetime", Helper.DATETIME_COL_WIDTH)
                + Helper.padRight("Tracking Id", Helper.TRACKING_ID_COL_WIDTH)
                + Helper.padRight("Filename", Helper.FILENAME_COL_WIDTH));

        int counter = 1;
        for (DeliveryReceipt receipt : receipts) {
            if (!existingTrackingIds.contains(receipt.getTrackingId())) {
                System.out.println(Helper.CONSOLE_COLOR_YELLOW
                        + Helper.padRight(String.valueOf(counter), Helper.EMPTY_COL_WIDTH)
                        + Helper.CONSOLE_COLOR_RESET + receipt.toPaddedString());
                counter++;
            }
        }
    }

    private static void assignTrackingIds(ArrayList<Transfer> transfers,
            List<Transfer> retoureTransfers, List<DeliveryReceipt> receipts) {
        printTrackingIdAssignmentDescr();
        Scanner scanner = new Scanner(System.in);
        HashSet<Integer> assignedNumbers = new HashSet<Integer>();
        boolean quit = false;
        outer: for (Transfer transfer : retoureTransfers) {
            boolean inputValid = false;
            while (!inputValid) {
                promptForTrackingIdAssignment(transfer);
                String input = scanner.next();
                try {
                    int number = Integer.parseInt(input);
                    DeliveryReceipt receipt = Helper.getDeliveryReceipt(receipts, number);
                    if (receipt != null && !assignedNumbers.contains(number)) {
                        // Assign the tracking id to the retoure transfer
                        transfer.setDeliveryReceipt(receipt);
                        receipt.setAssigned(true);
                        // An assigned tracking id is not allowed to be assigned twice
                        // Therefore, add it to the set of assigned numbers
                        // Notice, a number represents a tracking id
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
                    } else if (input.equals("q")) {
                        quit = true;
                        break outer;
                    }
                }
            }
        }
        scanner.close();
        if (!quit) {
            exportUnassignedDeliveryReceipts(receipts);
            exportTransfers(transfers);
        }
    }

    private static void printTrackingIdAssignmentDescr() {
        System.out.println();
        System.out.println(
                "Assign a tracking id to a retoure transfer or enter " + Helper.CONSOLE_COLOR_YELLOW
                        + "g" + Helper.CONSOLE_COLOR_RESET
                        + " for CSV generation or " + Helper.CONSOLE_COLOR_YELLOW + "s"
                        + Helper.CONSOLE_COLOR_RESET
                        + " to skip the retoure transfer or q to quit.");
    }

    private static void promptForTrackingIdAssignment(Transfer transfer) {
        System.out.print("Enter the " + Helper.CONSOLE_COLOR_YELLOW + "number"
                + Helper.CONSOLE_COLOR_RESET
                + " of the delivery receipt to assign its tracking id to retoure transfer "
                + Helper.CONSOLE_COLOR_CYAN + transfer.getPosition()
                + Helper.CONSOLE_COLOR_RESET
                + ": ");
    }

    private static void exportUnassignedDeliveryReceipts(List<DeliveryReceipt> receipts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (DeliveryReceipt receipt : receipts) {
            if (!receipt.isAssigned()) {
                stringBuilder.append(receipt.toCSVString() + "\n");
            }
        }
        String filename = Helper.PATH_TO_DELIVERY_RECEIPTS_EXPORT.replace("%DATETIME%",
                new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT_TIME)
                        .format(Calendar.getInstance().getTime()));
        generateCSV(stringBuilder, filename);
    }

    private static void exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Hash;Pos;Date;Balance;Retoure (Pos);BIC;IBAN;Name;Purpose;Tracking Id\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(transfer.toCSVString() + "\n");
        }
        String filename = Helper.PATH_TO_TRANSFERS_EXPORT.replace("%DATETIME%",
                new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT_TIME)
                        .format(Calendar.getInstance().getTime()));
        generateCSV(stringBuilder, filename);
    }

    private static void generateCSV(StringBuilder stringBuilder, String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.write(stringBuilder.toString());
            writer.close();
            System.out.println(
                    "Successfully exported to " + filename + ".");
        } catch (IOException e) {
            System.out.println(
                    "An error occurred while writing to " + filename + ".");
            e.printStackTrace();
        }
    }
}
