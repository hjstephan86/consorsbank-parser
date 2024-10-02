package com.consorsbank.parser.receipt;

public class TrackingIdForReceipt {


    private String trackingId;
    private DeliveryReceipt receipt;

    public TrackingIdForReceipt(String trackingId, DeliveryReceipt receipt) {
        this.trackingId = trackingId;
        this.receipt = receipt;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public DeliveryReceipt getReceipt() {
        return receipt;
    }
}
