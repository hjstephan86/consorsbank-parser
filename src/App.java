import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class App {

    public static void main(String[] args) throws Exception {
        parseArguments(args);

        File folder = new File(Helper.PATH_TO_PDF_REPORTS);
        File[] listOfFiles = folder.listFiles();

        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        readTransfers(listOfFiles, transfers);

        Collections.sort(transfers);
        setPosition(transfers);
        findRetoure(transfers);
        printTransfers(transfers);
        exportTransfers(transfers);
    }

    private static void setPosition(ArrayList<Transfer> transfers) {
        for (int i = 0; i < transfers.size(); i++) {
            Transfer transfer = transfers.get(i);
            transfer.setPosition(i + 1);
        }
    }

    private static void findRetoure(ArrayList<Transfer> transfers) {
        outerLoop: for (int i = transfers.size() - 1; i > 0; i--) {
            Transfer transfer = transfers.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Transfer prevTransfer = transfers.get(j);
                long daysBetween = ChronoUnit.DAYS.between(prevTransfer.getLocalDate(),
                        transfer.getLocalDate());
                if (daysBetween <= Helper.RETOURE_LIMIT_DAYS && !prevTransfer.isBalanced()) {
                    double balanceValue = transfer.getBalanceNumber().getValue();
                    double prevBalanceValue = prevTransfer.getBalanceNumber().getValue();

                    double epsilon = 1e-9;
                    if (balanceValue > 0 && prevBalanceValue < 0
                            && Math.abs(balanceValue - Math.abs(prevBalanceValue)) < epsilon
                            && transfer.getName().equals(prevTransfer.getName())) {
                        transfer.setRetoure(prevTransfer.getPosition());
                        prevTransfer.setBalanced(true);

                        // Continue with next transfer
                        continue outerLoop;
                    }
                }
            }
        }
    }

    private static void parseArguments(String[] args) {
        if (args.length == 2) {
            File pathToPDFReports = new File(args[0]);
            if (pathToPDFReports.exists() && pathToPDFReports.isDirectory() &&
                    args[1].toLowerCase().endsWith(".csv")) {
                Helper.PATH_TO_PDF_REPORTS = args[0];
                Helper.PATH_TO_CSV = args[1];
            }
        }
    }

    private static void printTransfers(ArrayList<Transfer> transfers) {
        double sum = 0;
        System.out.println(
                Helper.padRight("Pos", Helper.POS_COL_WIDTH)
                        + Helper.padRight("Date", Helper.DATE_COL_WIDTH)
                        + Helper.padRight("Balance", Helper.BALANCE_COL_WIDTH)
                        + Helper.padRight("Retoure (Pos)", Helper.RETOURE_COL_WIDTH)
                        + Helper.padRight("BIC", Helper.BIC_COL_WIDTH)
                        + Helper.padRight("IBAN", Helper.IBAN_COL_WIDTH)
                        + Helper.padRight("Name", Helper.NAME_COL_WIDTH)
                        + "Purpose");

        for (Transfer transfer : transfers) {
            System.out.println(transfer.toPaddedString());
            sum += transfer.getBalanceNumber().getValue();
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));
        System.out.println("Total sum: " + decimalFormat.format(sum));
    }

    private static void exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();

        int counter = 1;
        stringBuilder.append("Pos;Date;Balance;Retoure (Pos);BIC;IBAN;Name;Purpose\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(String.valueOf(counter++) + ";" + transfer.toCSVString() + "\n");
        }

        try {
            Helper.PATH_TO_CSV = Helper.PATH_TO_CSV.replace("%DATETIME%",
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                            .format(Calendar.getInstance().getTime()));
            FileWriter writer = new FileWriter(Helper.PATH_TO_CSV);
            writer.write(stringBuilder.toString());
            writer.close();
            System.out.println("Successfully exported transfers to " + Helper.PATH_TO_CSV + ".");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to " + Helper.PATH_TO_CSV + ".");
            e.printStackTrace();
        }
    }

    private static String getText(String filename) throws IOException {
        PDDocument pdDocument = Loader.loadPDF(new File(filename));
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        return pdfTextStripper.getText(pdDocument);
    }

    private static void readTransfers(File[] listOfFiles, ArrayList<Transfer> transfers)
            throws IOException {
        for (File f : listOfFiles) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                String pdfText = getText(f.getAbsolutePath());
                readTransfers(pdfText, transfers);
            }
        }
    }

    private static void readTransfers(String pdfText, ArrayList<Transfer> transfers) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        ArrayList<String> pdfTokens = new ArrayList<String>();
        StringTokenizer pdfTokenizer = new StringTokenizer(pdfText, "\n");
        while (pdfTokenizer.hasMoreTokens()) {
            pdfTokens.add(pdfTokenizer.nextToken());
        }

        Transfer transfer = null;
        int year = 2000;
        for (int i = 0; i < pdfTokens.size(); i++) {
            String line = pdfTokens.get(i);
            Pattern pattern = Pattern.compile(Helper.REGEX_TRANSFER_TYPES);
            Matcher matcher = pattern.matcher(line);

            if (line.startsWith(Helper.KONTOSTAND_ZUM_IN_TXT) && year == 2000) {
                year = parseYear(year, line);
                continue;
            }
            if (matcher.find()) {
                try {
                    transfer = parseBalanceAndDate(dateFormat, decimalFormat, pdfTokens, year, i);
                    transfers.add(transfer);
                    parseNameAndBankIDAndPurpose(pdfTokens, transfer, i);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int parseYear(int year, String line) {
        String[] strLine = line.split(" ");
        String[] strDate = strLine[2].split("\\.");
        year += Integer.parseInt(strDate[2]);
        return year;
    }

    private static Transfer parseBalanceAndDate(SimpleDateFormat dateFormat,
            DecimalFormat decimalFormat, ArrayList<String> pdfTokens, int year, int i)
            throws ParseException {
        String[] splittedLine = pdfTokens.get(i + 1).split(" ");
        Date date = dateFormat.parse(splittedLine[0] + year);
        Number number = decimalFormat.parse(splittedLine[3]);

        char sign = splittedLine[3].charAt(splittedLine[3].length() - 1);
        BalanceNumber balanceNumber = new BalanceNumber(number, sign, decimalFormat);

        Transfer transfer = new Transfer(balanceNumber, date);
        return transfer;
    }

    private static void parseNameAndBankIDAndPurpose(ArrayList<String> pdfTokens, Transfer transfer,
            int i) {
        if (bankIDExists(pdfTokens.get(i + 3))) {
            transfer.setName(pdfTokens.get(i + 2));
            transfer.setBankID(pdfTokens.get(i + 3));

            String purpose = pdfTokens.get(i + 4);
            if (!purpose.startsWith(Helper.INTERIM_KONTOSTAND_ZUM_IN_TXT)) {
                transfer.setPurpose(purpose);
            }
        } else {
            String purpose = pdfTokens.get(i + 2) + " " + pdfTokens.get(i + 3);
            transfer.setPurpose(purpose);
        }
    }

    private static boolean bankIDExists(String bankID) {
        // We expect a bank id like "<WELADED1WDB> DE33478535200003845849"
        // or "VISA 58525010 Paderborn"
        // or "girocard"

        String[] bankIDarr = bankID.split(" ");
        if (bankIDarr.length > 1) {
            String bicPattern = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";
            Pattern pattern = Pattern.compile(bicPattern);

            String bic = bankIDarr[0];
            bic = bic.replace("<", "").replace(">", "");
            Matcher matcher = pattern.matcher(bic);

            if (matcher.matches()) {
                // Case: "<WELADED1WDB> DE33478535200003845849"
                return true;
            } else {
                // Case: "VISA 58525010 Paderborn"
                // Simply return true
                return true;
            }
        }
        // Case: "girocard"
        return false;
    }
}
