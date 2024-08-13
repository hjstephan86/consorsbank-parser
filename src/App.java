import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class App {
    public static void main(String[] args) throws Exception {
        String file = "/home/stephan/Downloads/2023/KONTOAUSZUG_GIROKONTO_230583809_dat20230131_id1197004670.txt";
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();

        readTransfers(file, transfers);
        for (Transfer transfer : transfers) {
            System.out.println(transfer.toString());
        }
    }

    private static void readTransfers(String file, ArrayList<Transfer> transfers)
            throws IOException, FileNotFoundException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM");
            DecimalFormat balanceFormat = new DecimalFormat("0,00");

            Transfer transfer = null;
            boolean foundNewBlance = false;
            boolean foundSender = false;

            while ((line = br.readLine()) != null) {
                if (foundNewBlance) {
                    transfer.setSender(line);
                    foundNewBlance = false;
                    foundSender = true;
                    continue;
                }

                if (foundSender) {
                    transfer.setBankID(line);
                    foundSender = false;
                    continue;
                }

                String[] splittedLine = line.split(" ");
                if (splittedLine.length == 4) {
                    try {
                        Date date = dateFormat.parse(splittedLine[0]);
                        Number balance = balanceFormat.parse(splittedLine[3]);

                        foundNewBlance = true;
                        transfer = new Transfer(balance, date);
                        transfers.add(transfer);
                    } catch (ParseException e) {
                        continue;
                    }
                }
            }
        }
    }
}
