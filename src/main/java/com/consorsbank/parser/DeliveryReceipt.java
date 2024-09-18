package com.consorsbank.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeliveryReceipt implements Comparable<DeliveryReceipt> {

    private String sender;
    private String recipient;
    private String date;
    private String time;
    private LocalDateTime dateTime;
    private String trackingId;
    private String filename;
    private boolean assigned;

    private DateTimeFormatter readFormat;
    private DateTimeFormatter writeFormat;

    public DeliveryReceipt() {
        readFormat = DateTimeFormatter.ofPattern(Helper.DATETIME_FORMAT_READ);
        writeFormat = DateTimeFormatter.ofPattern(Helper.DATETIME_FORMAT_WRITE);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
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
        String dateTime = this.date + " " + this.time;
        this.dateTime = LocalDateTime.parse(dateTime, readFormat);
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDateTime(String dateTime) {
        // We use the write format here
        this.dateTime = LocalDateTime.parse(dateTime, writeFormat);
    }

    @Override
    public int compareTo(DeliveryReceipt o) {
        if (this.getDateTime().equals(o.getDateTime()))
            return 0;
        else
            return this.getDateTime().isBefore(o.getDateTime()) ? -1 : 1;
    }

    public String toPaddedString() {
        return Helper.padRight(
                Helper.truncate(
                        this.sender != null && !this.sender.toLowerCase().equals("null")
                                ? this.sender
                                : Helper.DELIVERY_RECEIPT_DEFAULT_SENDER,
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
                + Helper.padRight(this.trackingId != null ? this.trackingId : "",
                        Helper.TRACKING_ID_COL_WIDTH)
                + Helper.padRight(this.filename, Helper.FILENAME_COL_WIDTH);
    }

    public String toCSVString() {
        return (!this.sender.toLowerCase().equals("null") ? this.sender
                : Helper.DELIVERY_RECEIPT_DEFAULT_SENDER)
                + ";" + (!this.recipient.toLowerCase().equals("null") ? this.recipient : " ")
                + ";" + (this.dateTime != null ? this.writeFormat.format(this.dateTime) : " ")
                + ";" + this.trackingId
                + ";" + this.filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }
}
