package com.consorsbank.parser.retoure;

import com.consorsbank.parser.Transfer;

public class Packet implements Comparable<Packet> {
    private Transfer transfer;

    public Packet(Transfer transfer) {
        this.transfer = transfer;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public int compareTo(Packet o) {
        if (Math.abs(Math.abs(transfer.getBalanceNumber().getValue())
                - Math.abs(o.getTransfer().getBalanceNumber()
                        .getValue())) < com.consorsbank.parser.Helper.EPSILON)
            return 0;
        else
            return Math.abs(transfer.getBalanceNumber().getValue()) < Math
                    .abs(o.getTransfer().getBalanceNumber()
                            .getValue()) ? -1 : 1;
    }
}
