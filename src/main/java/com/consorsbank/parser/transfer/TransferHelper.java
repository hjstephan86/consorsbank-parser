package com.consorsbank.parser.transfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.consorsbank.parser.Helper;
import com.consorsbank.parser.retrn.ReturnWindow;

public class TransferHelper {
    public static ArrayList<Transfer> parseTransfers(File[] listOfFiles)
            throws IOException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        for (File f : listOfFiles) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                String text = Helper.getPDFReportText(f.getAbsolutePath());
                parseTransfers(text, transfers);
            }
        }
        return transfers;
    }

    private static void parseTransfers(String text, ArrayList<Transfer> transfers) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.SIMPLE_DATE_FORMAT);
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMAN));

        int positionInMonth = 0;
        ArrayList<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(text, "\n");
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }

        Transfer transfer = null;
        int year = 2000;
        for (int i = 0; i < tokens.size(); i++) {
            String line = tokens.get(i);
            Pattern pattern = Pattern.compile(Helper.PDF_REPORT_REGEX_TRANSFER_TYPES);
            Matcher matcher = pattern.matcher(line);

            if (line.startsWith(Helper.PDF_REPORT_KONTOSTAND_ZUM_IN_TXT) && year == 2000) {
                year = parseYear(year, line);
                continue;
            }
            if (matcher.find()) {
                try {
                    transfer =
                            parseBalanceAndDate(dateFormat, decimalFormat, tokens, year, i);
                    transfers.add(transfer);
                    positionInMonth++;
                    transfer.setPositionInMonth(positionInMonth);
                    parseNameAndBankIdAndPurpose(tokens, transfer, i);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Transfer parseBalanceAndDate(SimpleDateFormat dateFormat,
            DecimalFormat decimalFormat, ArrayList<String> pdfTokens, int year, int i)
            throws ParseException {
        String[] splittedLine = pdfTokens.get(i + 1).split(" ");
        Date date = dateFormat.parse(splittedLine[0] + year);
        Number number = decimalFormat.parse(splittedLine[3]);

        char sign = splittedLine[3].charAt(splittedLine[3].length() - 1);
        BalanceNumber balanceNumber = new BalanceNumber(number, sign);

        Transfer transfer = new Transfer(balanceNumber, date);
        return transfer;
    }

    private static int parseYear(int year, String line) {
        String[] strLine = line.split(" ");
        String[] strDate = strLine[2].split("\\.");
        year += Integer.parseInt(strDate[2]);
        return year;
    }

    /***
     * Parses the specified PDF tokens for a name and a bank id of the specified transfer.
     * The array list of tokens can contain the following string values:
     * 
     * ................................................................................
     * .....0.......LASTSCHRIFT..............................GEBUEHREN
     * .....1.......02.01. 8421 02.01. 393,00-...............28.06. 5910 28.06. 10,00-
     * .....2.......HUK-COBURG UNTERNEHMENSGRUPPE............Gebuehr fuer neue
     * .....3.......<BYLADEMM > DE14700500000002034343.......girocard
     * .....4.......HUK-COBURG, KRAFTFAHRT-VERS..............Konto/IBAN Auftragg. 09295
     * .....5.......36019 Konto/IBAN Empf. 027 0448588.................................
     * ................................................................................
     * 
     * @param pdfTokens the string array list of tokens to parse
     * @param transfer the transfer to parse for
     * @param i the index of the array list of tokens
     */
    private static void parseNameAndBankIdAndPurpose(ArrayList<String> pdfTokens, Transfer transfer,
            int i) {
        if (bankIdValid(pdfTokens.get(i + 3))) {
            transfer.setName(pdfTokens.get(i + 2));
            transfer.setBankID(pdfTokens.get(i + 3));

            String purpose = pdfTokens.get(i + 4);
            if (!purpose.startsWith(Helper.PDF_REPORT_INTERIM_KONTOSTAND_ZUM_IN_TXT)) {
                transfer.setPurpose(purpose);
            }
        } else {
            String purpose = pdfTokens.get(i + 2) + " " + pdfTokens.get(i + 3);
            transfer.setPurpose(purpose);
        }
    }

    public static boolean bankIdValid(String bankId) {
        String[] bankIdarr = bankId.split(" ");
        if (bankIdarr.length > 1) {
            String bicPattern = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";
            Pattern pattern = Pattern.compile(bicPattern);

            String bic = bankIdarr[0];
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

    public static LinkedHashMap<String, Transfer> setPosition(ArrayList<Transfer> transfers) {
        LinkedHashMap<String, Transfer> transferMap = new LinkedHashMap<String, Transfer>();
        for (int i = 0; i < transfers.size(); i++) {
            Transfer transfer = transfers.get(i);
            transfer.setPosition(i + 1);
            transfer.generateHash();
            transferMap.put(transfer.getHash(), transfer);
        }
        return transferMap;
    }

    public static LinkedHashMap<String, String> parseForExsistingTrackingIds(
            ArrayList<Transfer> transfers, LinkedHashMap<String, Transfer> transferMap) {
        LinkedHashMap<String, String> existingTrackingId2Transfer =
                new LinkedHashMap<String, String>();
        try (BufferedReader br =
                new BufferedReader(new FileReader(Helper.PATH_TO_TRANSFERS_IMPORT))) {
            System.out.println(
                    "Parse existing tracking ids from: " + Helper.PATH_TO_TRANSFERS_IMPORT);
            String line;
            while ((line = br.readLine()) != null) {
                String[] transferArr = line.split(";");
                String hashFromCSV = transferArr[0];
                if (transferArr.length == 10) {
                    String existingTrackingId = transferArr[9];
                    if (Helper.isTrackingIdValid(existingTrackingId)
                            && transferMap.containsKey(hashFromCSV)) {
                        Transfer returnTransfer = transferMap.get(hashFromCSV);
                        if (existingTrackingId.equals(Helper.RETURN_TRANSFER_NO_PACKAGE)) {
                            returnTransfer.setTrackingId(Helper.RETURN_TRANSFER_NO_PACKAGE);
                        } else {
                            returnTransfer.setExistingTrackingId(existingTrackingId);
                            existingTrackingId2Transfer.put(existingTrackingId,
                                    returnTransfer.getHash());
                            // Set the return postion
                            int returnPosition = Integer.parseInt(transferArr[4]);
                            if (returnPosition > 0 && returnPosition <= transfers.size()) {
                                Transfer otherTransfer = transfers.get(returnPosition - 1);
                                returnTransfer.setPointToTransfer(otherTransfer, false);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return existingTrackingId2Transfer;
    }

    public static StringBuilder exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Hash;Pos;Date;Balance;Return (Pos);BIC;IBAN;Name;Purpose;Tracking Id\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(transfer.toCSVString() + "\n");
        }
        return stringBuilder;
    }

    public static ArrayList<Transfer> getOpenTransfers(ArrayList<Transfer> transfers,
            ArrayList<ReturnWindow> returnWindows) {
        ArrayList<Transfer> openTransfers = new ArrayList<Transfer>();
        for (ReturnWindow returnWindow : returnWindows) {
            Date end = new Date();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(end);
            calendar.add(Calendar.DAY_OF_MONTH, -returnWindow.getWindow());
            Date start = calendar.getTime();

            List<Transfer> openTransfersForWindow =
                    transfers.stream()
                            .filter(transfer -> transfer.getName().toLowerCase()
                                    .contains(returnWindow.getSeller().toLowerCase())
                                    && transfer.getDate().after(start)
                                    && transfer.getDate().before(end)
                                    && transfer.getPointToTransfer() == null)
                            .collect(Collectors.toList());
            openTransfers.addAll(openTransfersForWindow);
        }
        return openTransfers;
    }
}
