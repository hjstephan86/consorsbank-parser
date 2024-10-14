package com.consorsbank.parser.retrn;

public class ReturnWindow {

    private String seller;
    private int window;

    public ReturnWindow(String seller, int window) {
        this.seller = seller;
        this.window = window;
    }

    public String getSeller() {
        return seller;
    }

    public int getWindow() {
        return window;
    }
}
