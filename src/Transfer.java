import java.text.SimpleDateFormat;
import java.util.Date;

public class Transfer {

    private Number balance;
    private Date date;
    private String sender;
    private String bankID;

    private SimpleDateFormat dateFormat;

    public Transfer(Number balance, Date date) {
        this.balance = balance;
        this.date = date;
        dateFormat = new SimpleDateFormat("dd.MM");
    }

    public Number getBalance() {
        return balance;
    }

    public void setBalance(Number balance) {
        this.balance = balance;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBankID() {
        return bankID;
    }

    public void setBankID(String bankID) {
        this.bankID = bankID;
    }

    public String toString() {
        return dateFormat.format(date) + " " + balance + " " + sender + " " + bankID;
    }
}
