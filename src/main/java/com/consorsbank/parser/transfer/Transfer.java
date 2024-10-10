package com.consorsbank.parser.transfer;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import com.consorsbank.parser.Helper;

public class Transfer implements Comparable<Transfer> {

    /**
     * The absolute position of this transfer
     */
    private int position;

    /**
     * The position of this transfer of this month, see {@link #date}
     */
    private int positionInMonth;
    private Date date;
    private BalanceNumber balanceNumber;

    private String bankID;
    private String BIC;
    private String IBAN;
    private String name;
    private String purpose;
    private String hash;
    private String existingTrackingId;
    private String trackingId;

    /**
     * The transfer to which this retoure transfer points to.
     */
    private Transfer pointToTransfer;
    private double retoureBalance;
    private LinkedHashMap<String, Transfer> incomingTransfers;

    private SimpleDateFormat dateFormat;

    public Transfer(BalanceNumber balanceNumber, Date date) {
        this.balanceNumber = balanceNumber;
        this.date = date;

        this.BIC = "";
        this.IBAN = "";
        this.name = "";
        this.purpose = "";
        this.retoureBalance = balanceNumber.getValue();

        this.dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        this.incomingTransfers = new LinkedHashMap<String, Transfer>();
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public BalanceNumber getBalanceNumber() {
        return balanceNumber;
    }

    public Date getDate() {
        return date;
    }

    public LocalDate getLocalDate() {
        return this.date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBankID(String bankID) {
        // Remove empty space
        this.bankID = bankID.replace(" >", ">");

        this.BIC = this.bankID.substring(0, this.bankID.indexOf(" "));
        this.IBAN = this.bankID.substring(this.bankID.indexOf(" ") + 1);
    }

    public String getPaddedPosistion() {
        return Helper.padRight(String.valueOf(this.position), Helper.POS_COL_WIDTH);
    }

    public String toPaddedString() {
        return Helper.padRight(dateFormat.format(this.date), Helper.DATE_COL_WIDTH)
                + Helper.padRight(String.format("%12.2f", this.balanceNumber.getValue()),
                        Helper.BALANCE_COL_WIDTH)
                + Helper.padRight(
                        String.valueOf(this.pointToTransfer != null
                                ? this.pointToTransfer.getPosition()
                                : ""),
                        Helper.RETOURE_COL_WIDTH)
                + Helper.padRight(this.BIC, Helper.BIC_COL_WIDTH)
                + Helper.padRight(this.IBAN, Helper.IBAN_COL_WIDTH)
                + Helper.padRight(
                        Helper.truncate(this.name,
                                Helper.NAME_COL_WIDTH - Helper.TRUNCATE_COL_WIDTH_DELTA),
                        Helper.NAME_COL_WIDTH)
                + this.purpose;
    }

    public String toCSVString() {
        return this.getHash()
                + ";" + this.position
                + ";" + this.dateFormat.format(this.date)
                + ";" + this.balanceNumber.toString()
                + ";"
                + (this.pointToTransfer != null ? this.pointToTransfer.getPosition()
                        : "")
                + ";" + this.BIC
                + ";" + this.IBAN
                + ";" + this.name
                + ";" + this.purpose
                + ";" + this.getTrackingId();
    }

    @Override
    public int compareTo(Transfer o) {
        if (this.getDate().equals(o.getDate()))
            return 0;
        else
            return this.getDate().before(o.getDate()) ? -1 : 1;
    }

    public String getPurpose() {
        return this.purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setPointToTransfer(Transfer transfer, boolean findPotentialRetoureTransfer) {
        this.pointToTransfer = transfer;
        if (pointToTransfer != null && !findPotentialRetoureTransfer) {
            // Update the retoure balance value of the pointed transfer
            double retoureBalance =
                    pointToTransfer.getRetoureBalance() + this.getBalanceNumber().getValue();
            pointToTransfer.setRetoureBalance(retoureBalance);
        }
    }

    public Transfer getPointToTransfer() {
        return pointToTransfer;
    }

    public LinkedHashMap<String, Transfer> getIncomingTransfers() {
        return incomingTransfers;
    }

    public double getRetoureBalance() {
        return retoureBalance;
    }

    public void setRetoureBalance(double balanceValue) {
        this.retoureBalance = balanceValue;
    }

    public String getExistingTrackingId() {
        return this.existingTrackingId;
    }

    public void setExistingTrackingId(String existingTrackingId) {
        this.existingTrackingId = existingTrackingId;
    }

    private String getTrackingId() {
        return existingTrackingId != null ? existingTrackingId
                : (trackingId != null)
                        ? trackingId
                        : "";
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public void generateHash() {
        String input = String.valueOf(this.positionInMonth)
                + this.date
                + String.valueOf(this.balanceNumber.getValue())
                + this.BIC
                + this.IBAN
                + this.name
                + this.purpose;

        this.hash = Helper.getHash(input);
    }

    public String getHash() {
        return this.hash;
    }

    public int getPositionInMonth() {
        return positionInMonth;
    }

    public void setPositionInMonth(int positionInMonth) {
        this.positionInMonth = positionInMonth;
    }
}
