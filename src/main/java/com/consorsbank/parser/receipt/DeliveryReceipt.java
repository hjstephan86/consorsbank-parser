package com.consorsbank.parser.receipt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import com.consorsbank.parser.Helper;
import com.consorsbank.parser.transfer.Transfer;

public class DeliveryReceipt implements Comparable<DeliveryReceipt> {

    private String sender;
    private String recipient;
    private String date;
    private String time;
    private LocalDateTime dateTime;
    private LinkedHashSet<String> trackingIds;
    private String filename;
    private String fileHash;

    private LinkedHashMap<String, Transfer> trackingIdToTransfer;

    private DateTimeFormatter readFormat;
    private DateTimeFormatter writeFormat;

    public DeliveryReceipt() {
        this.readFormat = DateTimeFormatter.ofPattern(Helper.DATETIME_FORMAT_READ);
        this.writeFormat = DateTimeFormatter.ofPattern(Helper.DATETIME_FORMAT_WRITE);
        this.trackingIds = new LinkedHashSet<String>();
        this.trackingIdToTransfer = new LinkedHashMap<String, Transfer>();
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public HashSet<String> getTrackingIds() {
        return this.trackingIds;
    }

    public void addTrackingId(String trackingId) {
        this.trackingIds.add(trackingId);
    }

    public boolean hasTrackingId() {
        return this.trackingIds.size() > 0;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setTime(String time) {
        this.time = time;
        // Consider also '2024-09-16 null'
        String strDateTime = "";
        if (this.time.equals("null")) {
            strDateTime = this.date + " " + "00:00:00";
        } else {
            strDateTime = this.date + " " + this.time;
        }
        this.dateTime = LocalDateTime.parse(strDateTime, readFormat);
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDateTime(String dateTime) {
        // We use the write format here to read the date time, since we used the write format to
        // write the date time
        this.dateTime = LocalDateTime.parse(dateTime, writeFormat);
    }

    @Override
    public int compareTo(DeliveryReceipt o) {
        if (this.getDateTime().equals(o.getDateTime()))
            return 0;
        else
            return this.getDateTime().isBefore(o.getDateTime()) ? -1 : 1;
    }

    public String getPaddedStringForTrackingId(String trackingId) {
        return Helper.padRight(
                Helper.truncate(
                        this.sender != null && !this.sender.toLowerCase().equals("null")
                                ? this.sender
                                : (Helper.isHermesTrackingId(trackingId)
                                        ? Helper.DELIVERY_RECEIPT_HERMES_SENDER
                                        : Helper.DELIVERY_RECEIPT_DHL_SENDER),
                        Helper.SENDER_COL_WIDTH - Helper.TRUNCATE_COL_WIDTH_DELTA),
                Helper.SENDER_COL_WIDTH)
                + Helper.padRight(
                        Helper.truncate(
                                this.recipient != null
                                        && !this.recipient.toLowerCase().equals("null")
                                                ? this.recipient
                                                : "",
                                Helper.RECEPIENT_COL_WIDTH - Helper.TRUNCATE_COL_WIDTH_DELTA),
                        Helper.RECEPIENT_COL_WIDTH)
                + Helper.padRight(
                        this.dateTime != null ? this.writeFormat.format(this.dateTime) : "",
                        Helper.DATETIME_COL_WIDTH)
                + Helper.padRight(trackingId != null ? trackingId : "",
                        Helper.TRACKING_ID_COL_WIDTH)
                + Helper.padRight(this.filename, Helper.FILENAME_COL_WIDTH);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void addTrackingId(String trackingId, Transfer transfer) {
        if (trackingIds.contains(trackingId) && transfer != null) {
            trackingIdToTransfer.put(trackingId, transfer);
        }
    }

    public boolean allTrackingIdsAssigned() {
        return trackingIds.size() == trackingIdToTransfer.size();
    }

    public String toCSVString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String trackingId : trackingIds) {
            String line =
                    this.fileHash
                            + ";" + this.filename
                            + ";" + (!this.sender.toLowerCase().equals("null") ? this.sender
                                    : (Helper.isHermesTrackingId(trackingId)
                                            ? Helper.DELIVERY_RECEIPT_HERMES_SENDER
                                            : Helper.DELIVERY_RECEIPT_DHL_SENDER))
                            + ";"
                            + (!this.recipient.toLowerCase().equals("null") ? this.recipient : " ")
                            + ";"
                            + (this.dateTime != null ? this.writeFormat.format(this.dateTime) : " ")
                            + ";" + trackingId
                            + ";" + (this.trackingIdToTransfer.containsKey(trackingId)
                                    ? this.trackingIdToTransfer.get(trackingId).getHash()
                                    : "");
            stringBuilder.append(line + "\n");
        }
        return stringBuilder.toString();
    }
}
