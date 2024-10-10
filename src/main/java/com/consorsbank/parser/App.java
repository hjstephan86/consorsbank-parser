package com.consorsbank.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.consorsbank.parser.receipt.DeliveryReceipt;
import com.consorsbank.parser.receipt.ReceiptHelper;
import com.consorsbank.parser.receipt.TrackingIdForReceipt;
import com.consorsbank.parser.retoure.RetoureHelper;
import com.consorsbank.parser.transfer.Transfer;
import com.consorsbank.parser.transfer.TransferHelper;

public class App {

    public static void main(String[] args) throws Exception {
        if (argumentsValid(args)) {
            File folder = new File(Helper.PATH_TO_PDF_REPORTS);
            File[] listOfPDFReportFiles = folder.listFiles();
            ArrayList<Transfer> transfers = TransferHelper.parseTransfers(listOfPDFReportFiles);
            Collections.sort(transfers);
            LinkedHashMap<String, Transfer> transferMap = TransferHelper.setPosition(transfers);
            RetoureHelper.findRetoureTransfers(transfers);
            RetoureHelper.packageRetoureTransfers(transfers, transferMap);

            HashSet<String> existingTrackingIds =
                    TransferHelper.parseForExsistingTrackingIds(transferMap);
            List<Transfer> retoureTransfers =
                    transfers.stream()
                            .filter(transfer -> transfer.getPointToTransfer() != null
                                    && transfer.getExistingTrackingId() == null)
                            .collect(Collectors.toList());

            LinkedHashMap<String, DeliveryReceipt> existingReceipts =
                    ReceiptHelper.parseForExistingDeliveryReceipts(transferMap);
            folder = new File(Helper.PATH_TO_DELIVERY_RECEIPTS);
            File[] listOfReceiptFiles = folder.listFiles();
            listOfReceiptFiles =
                    ReceiptHelper.removeExistingDeliveryReceipts(listOfReceiptFiles,
                            existingReceipts);
            HashMap<Integer, DeliveryReceipt> receiptMap =
                    ReceiptHelper.parseDeliveryReceipts(listOfReceiptFiles);
            ArrayList<DeliveryReceipt> receipts =
                    new ArrayList<DeliveryReceipt>(receiptMap.values());

            printTransfers(transfers, retoureTransfers);
            List<DeliveryReceipt> allReceipts =
                    Stream.concat(receipts.stream(), existingReceipts.values().stream())
                            .collect(Collectors.toList());
            Collections.sort(allReceipts);
            printReceipts(allReceipts, existingTrackingIds);

            assignTrackingIdsAndExport(transfers, retoureTransfers, allReceipts,
                    existingTrackingIds);
        }
    }

    private static boolean argumentsValid(String[] args) {
        boolean argumentsValid = false;
        switch (args.length) {
            case 2:
                argumentsValid = parsePDFsAndReceiptsPathsArgs(args);
                break;
            case 3:
                argumentsValid = parsePDFsAndReceiptsPathsArgs(args)
                        && parseArgTransferImport(args);
                break;
            case 4:
                argumentsValid = parsePDFsAndReceiptsPathsArgs(args)
                        && parseArgTransferImport(args)
                        && parseArgTransferExport(args);
                break;
            default:
                break;
        }
        if (!argumentsValid) {
            File pathToPDFReports = new File(Helper.PATH_TO_PDF_REPORTS);
            File pathToDeliveryReceipts = new File(Helper.PATH_TO_DELIVERY_RECEIPTS);
            if (pathToPDFReports.exists() && pathToPDFReports.isDirectory()
                    && pathToDeliveryReceipts.exists() && pathToDeliveryReceipts.isDirectory()) {
                return true;
            }
        }
        return argumentsValid;
    }

    private static boolean parsePDFsAndReceiptsPathsArgs(String[] args) {
        File pathToPDFReports = new File(args[0]);
        File pathToDeliveryReceipts = new File(args[1]);
        if (pathToPDFReports.exists() && pathToPDFReports.isDirectory()
                && pathToDeliveryReceipts.exists() && pathToDeliveryReceipts.isDirectory()) {
            Helper.PATH_TO_PDF_REPORTS = args[0];
            Helper.PATH_TO_DELIVERY_RECEIPTS = args[1];
            // Update the CSV file path accordingly
            Helper.PATH_TO_DELIVERY_RECEIPTS_FILE =
                    Helper.PATH_TO_DELIVERY_RECEIPTS + Helper.PATH_TO_DELIVERY_RECEIPTS_FILE_NAME;
            return true;
        }
        return false;
    }

    private static boolean parseArgTransferImport(String[] args) {
        File pathToTransfersImport = new File(args[2]);
        if (pathToTransfersImport.exists() && pathToTransfersImport.isFile()
                && pathToTransfersImport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_TRANSFERS_IMPORT = pathToTransfersImport.getAbsolutePath();
            return true;
        }
        return false;
    }

    private static boolean parseArgTransferExport(String[] args) {
        File pathToTransfersExport = new File(args[3]);
        if (pathToTransfersExport.getName().toLowerCase().endsWith(".csv")) {
            Helper.PATH_TO_TRANSFERS_EXPORT = pathToTransfersExport.getAbsolutePath();
            return true;
        }
        return true;
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
            for (String trackingId : receipt.getTrackingIds()) {
                if (!existingTrackingIds.contains(trackingId)) {
                    System.out.println(Helper.CONSOLE_COLOR_YELLOW
                            + Helper.padRight(String.valueOf(counter), Helper.EMPTY_COL_WIDTH)
                            + Helper.CONSOLE_COLOR_RESET
                            + (counter % 2 != 0 ? Helper.CONSOLE_COLOR_GRAY
                                    : Helper.CONSOLE_COLOR_RESET)
                            + receipt.getPaddedStringForTrackingId(trackingId)
                            + Helper.CONSOLE_COLOR_RESET);
                    counter++;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static void assignTrackingIdsAndExport(ArrayList<Transfer> transfers,
            List<Transfer> retoureTransfers, List<DeliveryReceipt> receipts,
            HashSet<String> existingTrackingIds) {
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
                    TrackingIdForReceipt trackingIdForReceipt =
                            Helper.getTrackingIdForReceipt(receipts, number, existingTrackingIds);
                    if (trackingIdForReceipt != null && !assignedNumbers.contains(number)) {
                        // Assign the tracking id to the retoure transfer
                        transfer.setTrackingId(trackingIdForReceipt.getTrackingId());
                        // Add tracking id assignment to the delivery receipt
                        trackingIdForReceipt.getReceipt()
                                .addTrackingId(trackingIdForReceipt.getTrackingId(), transfer);
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
            exportTransfers(transfers);
        }
        exportDeliveryReceipts(receipts);
    }

    private static void exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = TransferHelper.exportTransfers(transfers);
        String filename = Helper.PATH_TO_TRANSFERS_EXPORT.replace("%DATETIME%",
                new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT_TIME)
                        .format(Calendar.getInstance().getTime()));
        generateCSV(stringBuilder, filename);
    }

    private static void exportDeliveryReceipts(List<DeliveryReceipt> receipts) {
        generateCSV(ReceiptHelper.exportDeliveryReceipts(receipts),
                Helper.PATH_TO_DELIVERY_RECEIPTS_FILE);
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
                + " of the tracking id to assign it to retoure transfer "
                + Helper.CONSOLE_COLOR_CYAN + transfer.getPosition()
                + Helper.CONSOLE_COLOR_RESET
                + ": ");
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
