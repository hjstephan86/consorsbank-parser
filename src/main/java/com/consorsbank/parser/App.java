package com.consorsbank.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.consorsbank.parser.retrn.ReturnHelper;
import com.consorsbank.parser.retrn.ReturnWindow;
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
            ReturnHelper.findReturnTransfers(transfers);
            ReturnHelper.packageReturnTransfers(transfers, transferMap);
            ArrayList<ReturnWindow> returnWindows = ReturnHelper.readSellers();
            ArrayList<Transfer> openTransfers =
                    TransferHelper.getOpenTransfers(transfers, returnWindows);
            Collections.sort(openTransfers);

            LinkedHashMap<String, String> existingTrackingId2Transfer =
                    TransferHelper.parseForExsistingTrackingIds(transfers, transferMap);
            List<Transfer> returnTransfers =
                    transfers.stream()
                            .filter(transfer -> transfer.getPointToTransfer() != null
                                    && transfer.getExistingTrackingId() == null
                                    && !transfer.getTrackingId()
                                            .equals(Helper.RETURN_TRANSFER_NO_PACKAGE))
                            .collect(Collectors.toList());

            LinkedHashMap<String, DeliveryReceipt> existingReceipts =
                    ReceiptHelper.parseForExistingDeliveryReceipts(transferMap,
                            existingTrackingId2Transfer);
            folder = new File(Helper.PATH_TO_DELIVERY_RECEIPTS);
            File[] listOfReceiptFiles = folder.listFiles();
            listOfReceiptFiles =
                    ReceiptHelper.removeExistingDeliveryReceipts(listOfReceiptFiles,
                            existingReceipts);
            HashMap<Integer, DeliveryReceipt> receiptMap =
                    ReceiptHelper.parseDeliveryReceipts(listOfReceiptFiles);
            ArrayList<DeliveryReceipt> receipts =
                    new ArrayList<DeliveryReceipt>(receiptMap.values());

            printTransfers(transfers, openTransfers, returnTransfers);
            List<DeliveryReceipt> allReceipts =
                    Stream.concat(receipts.stream(), existingReceipts.values().stream())
                            .collect(Collectors.toList());
            Collections.sort(allReceipts);
            printReceipts(allReceipts, existingTrackingId2Transfer);
            System.out.println();
            assignTrackingIdsAndExport(transfers, returnTransfers, allReceipts,
                    existingTrackingId2Transfer);
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
            // Update the delivery receipts file path accordingly
            Helper.PATH_TO_DELIVERY_RECEIPTS_FILE =
                    Helper.PATH_TO_DELIVERY_RECEIPTS + Helper.PATH_TO_DELIVERY_RECEIPTS_FILE_NAME;
            Helper.PATH_TO_DELIVERY_RECEIPTS_ASSIGNED_FOLDER =
                    Helper.PATH_TO_DELIVERY_RECEIPTS + Helper.DELIVERY_RECEIPTS_ASSIGNED_FOLDER;
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
            List<Transfer> openTransfers, List<Transfer> returnTransfers) {
        System.out.println();
        System.out
                .println(Helper.CONSOLE_COLOR_BLUE_BOLD + "Transfers" + Helper.CONSOLE_COLOR_RESET);
        printTransfers(transfers, false);

        System.out.println();
        System.out.println(
                Helper.CONSOLE_COLOR_BLUE_BOLD + "Open Transfers" + Helper.CONSOLE_COLOR_RESET);
        printTransfers(openTransfers, false);

        System.out.println();
        System.out.println(
                Helper.CONSOLE_COLOR_BLUE_BOLD + "Return Transfers" + Helper.CONSOLE_COLOR_RESET);
        printTransfers(returnTransfers, true);
    }

    private static void printTransfers(List<Transfer> transfers, boolean color) {
        double sum = 0;
        System.out.println(Helper.CONSOLE_COLOR_GRAY_BOLD
                + Helper.padRight("Pos", Helper.POS_COL_WIDTH)
                + Helper.padRight("Date", Helper.DATE_COL_WIDTH)
                + Helper.padRight("Balance", Helper.BALANCE_COL_WIDTH)
                + Helper.padRight("Return (Pos)", Helper.RETURN_COL_WIDTH)
                + Helper.padRight("BIC", Helper.BIC_COL_WIDTH)
                + Helper.padRight("IBAN", Helper.IBAN_COL_WIDTH)
                + Helper.padRight("Name", Helper.NAME_COL_WIDTH)
                + "Purpose"
                + Helper.CONSOLE_COLOR_RESET);

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
        String strSum = decimalFormat.format(sum);
        System.out
                .println(
                        "Total sum: "
                                + (sum >= 0 ? Helper.CONSOLE_COLOR_GREEN + strSum
                                        : Helper.CONSOLE_COLOR_RED + strSum)
                                + Helper.CONSOLE_COLOR_RESET);
    }

    private static void printReceipts(List<DeliveryReceipt> receipts,
            LinkedHashMap<String, String> existingTrackingId2Transfer) {
        System.out.println();
        System.out.println(
                Helper.CONSOLE_COLOR_BLUE_BOLD + "Delivery Receipts" + Helper.CONSOLE_COLOR_RESET);
        System.out.println(Helper.CONSOLE_COLOR_GRAY_BOLD
                + Helper.padRight("", Helper.EMPTY_COL_WIDTH)
                + Helper.padRight("Sender", Helper.SENDER_COL_WIDTH)
                + Helper.padRight("Recipient", Helper.RECEPIENT_COL_WIDTH)
                + Helper.padRight("Datetime", Helper.DATETIME_COL_WIDTH)
                + Helper.padRight("Tracking Id", Helper.TRACKING_ID_COL_WIDTH)
                + Helper.padRight("Filename", Helper.FILENAME_COL_WIDTH)
                + Helper.CONSOLE_COLOR_RESET);

        int counter = 1;
        for (DeliveryReceipt receipt : receipts) {
            for (String trackingId : receipt.getTrackingIds()) {
                if (existingTrackingId2Transfer.get(trackingId) == null) {
                    System.out.println(Helper.CONSOLE_COLOR_YELLOW
                            + Helper.padRight(String.valueOf(counter), Helper.EMPTY_COL_WIDTH)
                            + Helper.CONSOLE_COLOR_RESET
                            + (counter % 2 == 0 ? Helper.CONSOLE_COLOR_GRAY
                                    : Helper.CONSOLE_COLOR_RESET)
                            + receipt.getPaddedStringForTrackingId(trackingId)
                            + Helper.CONSOLE_COLOR_RESET);
                    counter++;
                }
            }
        }
    }

    private static void assignTrackingIdsAndExport(ArrayList<Transfer> transfers,
            List<Transfer> returnTransfers, List<DeliveryReceipt> receipts,
            LinkedHashMap<String, String> existingTrackingId2Transfer) {
        printTrackingIdAssignmentDescr();
        Scanner scanner = new Scanner(System.in);
        HashSet<Integer> assignedNumbers = new HashSet<Integer>();
        boolean quit = false;
        outer: for (Transfer transfer : returnTransfers) {
            boolean inputValid = false;
            while (!inputValid) {
                promptForTrackingIdAssignment(transfer);
                String input = scanner.next();
                try {
                    int number = Integer.parseInt(input);
                    TrackingIdForReceipt trackingIdForReceipt =
                            Helper.getTrackingIdForReceipt(receipts, number,
                                    existingTrackingId2Transfer);
                    if (trackingIdForReceipt != null && !assignedNumbers.contains(number)) {
                        // Assign the tracking id to the return transfer
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
                    } else if (input.equals("n")) {
                        transfer.setTrackingId(Helper.RETURN_TRANSFER_NO_PACKAGE);
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
        System.out.println();

        if (!quit) {
            exportTransfers(transfers);
        }
        moveAssignedDeliveryReceipts(receipts);
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
        System.out.println(
                "Assign a tracking id to a return transfer or enter "
                        + Helper.CONSOLE_COLOR_YELLOW + "g" + Helper.CONSOLE_COLOR_RESET
                        + " for CSV generation or "
                        + Helper.CONSOLE_COLOR_YELLOW + "n" + Helper.CONSOLE_COLOR_RESET
                        + " for no tracking id available or "
                        + Helper.CONSOLE_COLOR_YELLOW + "s" + Helper.CONSOLE_COLOR_RESET
                        + " to skip the return transfer or"
                        + " q to quit.");
    }

    private static void promptForTrackingIdAssignment(Transfer transfer) {
        System.out.print("Enter the " + Helper.CONSOLE_COLOR_YELLOW + "number"
                + Helper.CONSOLE_COLOR_RESET
                + " of the tracking id to assign it to return transfer "
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

    private static void moveAssignedDeliveryReceipts(List<DeliveryReceipt> receipts) {
        File assignedFolder = new File(Helper.PATH_TO_DELIVERY_RECEIPTS_ASSIGNED_FOLDER);
        if (!assignedFolder.exists()
                && receipts.stream().anyMatch(receipt -> receipt.allTrackingIdsAssigned())) {
            System.out.println("Assigned folder for delivery receipts does not exist. Creating...");
            assignedFolder.mkdir();
        }

        int movedReceipts = 0;
        for (DeliveryReceipt receipt : receipts) {
            if (receipt.allTrackingIdsAssigned()) {
                File file = new File(Helper.PATH_TO_DELIVERY_RECEIPTS + receipt.getFilename());
                if (file.isFile()) {
                    Path sourcePath = Paths.get(file.getAbsolutePath());
                    Path targetPath = Paths.get(assignedFolder.getAbsolutePath(), file.getName());

                    try {
                        Files.move(sourcePath, targetPath);
                        // System.out.println("Moved " + file.getName() + " to assigned folder.");
                        movedReceipts++;
                    } catch (IOException e) {
                        System.err.println("Error moving file: " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("Moved " + movedReceipts + " delivery receipt(s) to assigned folder.");
    }
}
