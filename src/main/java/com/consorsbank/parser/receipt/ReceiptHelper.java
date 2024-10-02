package com.consorsbank.parser.receipt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import com.consorsbank.parser.Helper;
import com.consorsbank.parser.transfer.Transfer;

public class ReceiptHelper {

    public static HashMap<Integer, DeliveryReceipt> parseDeliveryReceipts(File[] listOfFiles)
            throws IOException, InterruptedException {
        HashMap<Integer, DeliveryReceipt> receiptMap = new HashMap<Integer, DeliveryReceipt>();
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
                            && receipt.hasTrackingId()) {
                        if (!receiptMap.containsKey(receipt.hashCode())) {
                            receiptMap.put(receipt.hashCode(), receipt);
                            receipt.setFilename(f.getName());
                            receipt.setFileHash(Helper.getFileChecksum(Helper.SHA_ALGORITHM,
                                    f.getAbsolutePath()));
                        }
                    }
                }
            }
        }
        return receiptMap;
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
            String trackingIdValue =
                    token.substring(token.indexOf("=") + 1, token.lastIndexOf("}"));
            String[] trackingIDValueArr = trackingIdValue.split("[\\{\\},= ]");
            for (String trackingIdValueArrEntry : trackingIDValueArr) {
                if (Helper.trackingIdIsValid(trackingIdValueArrEntry)) {
                    receipt.addTrackingId(trackingIdValueArrEntry);
                }
            }
        }
        return receipt;
    }

    public static LinkedHashMap<String, DeliveryReceipt> parseForExistingDeliveryReceipts(
            LinkedHashMap<String, Transfer> transferMap) {
        LinkedHashMap<String, DeliveryReceipt> existingDeliveryReceipts =
                new LinkedHashMap<String, DeliveryReceipt>();
        File deliveryReceiptsCSV = new File(Helper.PATH_TO_DELIVERY_RECEIPTS_FILE);
        if (deliveryReceiptsCSV.exists()) {
            try (BufferedReader br =
                    new BufferedReader(new FileReader(deliveryReceiptsCSV.getAbsolutePath()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] receiptArr = line.split(";");
                    if (receiptArr.length == 6 || receiptArr.length == 7) {
                        String trackingId = receiptArr[5];
                        if (Helper.trackingIdIsValid(trackingId)) {
                            String fileHash = receiptArr[0];

                            DeliveryReceipt receipt = null;
                            if (existingDeliveryReceipts.containsKey(fileHash)) {
                                receipt = existingDeliveryReceipts.get(fileHash);
                            } else {
                                receipt = new DeliveryReceipt();
                                existingDeliveryReceipts.put(fileHash, receipt);
                            }

                            receipt.setFileHash(fileHash);
                            receipt.setFilename(receiptArr[1]);
                            receipt.setSender(receiptArr[2]);
                            receipt.setRecipient(receiptArr[3]);
                            receipt.setDateTime(receiptArr[4]);
                            receipt.addTrackingId(trackingId);
                            if (receiptArr.length == 7) {
                                receipt.addTrackingId(trackingId, transferMap.get(receiptArr[6]));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return existingDeliveryReceipts;
    }

    public static File[] removeExistingDeliveryReceipts(File[] listOfReceiptFiles,
            LinkedHashMap<String, DeliveryReceipt> existingReceipts) {

        ArrayList<File> listOfRemainingReceiptFiles = new ArrayList<File>();
        HashSet<String> existingDeliveryReceiptFileHashes = new HashSet<String>();
        for (DeliveryReceipt receipt : existingReceipts.values()) {
            existingDeliveryReceiptFileHashes.add(receipt.getFileHash());
        }

        for (int i = 0; i < listOfReceiptFiles.length; i++) {
            File f = listOfReceiptFiles[i];
            if (f.isFile() && (f.getName().toLowerCase().endsWith(".pdf")
                    || f.getName().toLowerCase().endsWith(".jpg")
                    || f.getName().toLowerCase().endsWith(".jpeg"))) {
                String fileHash = Helper.getFileChecksum(Helper.SHA_ALGORITHM, f.getAbsolutePath());
                if (!existingDeliveryReceiptFileHashes.contains(fileHash)) {
                    listOfRemainingReceiptFiles.add(listOfReceiptFiles[i]);
                } else {
                    if (!f.getName().equals(existingReceipts.get(fileHash).getFilename())) {
                        // Update the filename of the existing delivery receipt
                        existingReceipts.get(fileHash).setFilename(f.getName());
                    }
                }
            }
        }
        return listOfRemainingReceiptFiles.stream().toArray(File[]::new);
    }

    public static StringBuilder exportDeliveryReceipts(List<DeliveryReceipt> receipts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (DeliveryReceipt receipt : receipts) {
            stringBuilder.append(receipt.toCSVString());
        }
        return stringBuilder;
    }
}
