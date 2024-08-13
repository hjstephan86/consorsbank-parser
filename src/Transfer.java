import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class Transfer implements Comparable<Transfer> {

    private int position;
    private Date date;
    private BalanceNumber balanceNumber;
    private int retoure;
    private boolean isBalanced;
    private String bankID;
    private String BIC;
    private String IBAN;
    private String name;
    private String purpose;

    private SimpleDateFormat dateFormat;

    public Transfer(BalanceNumber balanceNumber, Date date) {
        this.balanceNumber = balanceNumber;
        this.date = date;

        this.BIC = "";
        this.IBAN = "";
        this.name = "";
        this.purpose = "";

        dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
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

    public String toPaddedString() {
        return Helper.padRight(dateFormat.format(this.date), Helper.DATE_COL_WIDTH)
                + Helper.padRight(this.balanceNumber.toString(), Helper.BALANCE_COL_WIDTH)
                + Helper.padRight(String.valueOf(this.retoure), Helper.RETOURE_COL_WIDTH)
                + Helper.padRight(this.BIC, Helper.BIC_COL_WIDTH)
                + Helper.padRight(this.IBAN, Helper.IBAN_COL_WIDTH)
                + Helper.padRight(this.name, Helper.NAME_COL_WIDTH)
                + this.purpose;
    }

    public String toCSVString() {
        return dateFormat.format(this.date)
                + ";" + this.balanceNumber.toString()
                + ";" + this.retoure
                + ";" + this.BIC
                + ";" + this.IBAN
                + ";" + this.name
                + ";" + this.purpose;
    }

    @Override
    public int compareTo(Transfer o) {
        if (this.getDate().equals(o.getDate()))
            return 0;
        else
            return this.getDate().before(o.getDate()) ? -1 : 1;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isBalanced() {
        return isBalanced;
    }

    public void setBalanced(boolean isBalanced) {
        this.isBalanced = isBalanced;
    }

    public void setRetoure(int retoure) {
        this.retoure = retoure;
    }
}
