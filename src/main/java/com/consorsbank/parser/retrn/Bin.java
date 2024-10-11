package com.consorsbank.parser.retrn;

import com.consorsbank.parser.transfer.Transfer;

public class Bin implements Comparable<Bin> {

    private Transfer transfer;

    public Bin(Transfer transfer) {
        this.transfer = transfer;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public int compareTo(Bin o) {
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
