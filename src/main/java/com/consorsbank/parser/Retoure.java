package com.consorsbank.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Retoure implements Comparable<Retoure> {

    private String trackingId;
    private String recipient;
    private String sender;
    private String date;
    private String time;
    private LocalDateTime dateTime;

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        this.dateTime = localDateTime;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public int compareTo(Retoure o) {
        if (this.getDateTime().equals(o.getDateTime()))
            return 0;
        else
            return this.getDateTime().isBefore(o.getDateTime()) ? -1 : 1;
    }
}
