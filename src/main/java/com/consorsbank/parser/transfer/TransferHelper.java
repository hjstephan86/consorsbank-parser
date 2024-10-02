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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.consorsbank.parser.Helper;

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
                            Helper.parseBalanceAndDate(dateFormat, decimalFormat, tokens, year, i);
                    transfers.add(transfer);
                    positionInMonth++;
                    transfer.setPositionInMonth(positionInMonth);
                    Helper.parseNameAndBankIdAndPurpose(tokens, transfer, i);
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

    public static HashSet<String> parseForExsistingTrackingIds(
            LinkedHashMap<String, Transfer> transferMap) {
        HashSet<String> existingTrackingIds = new HashSet<String>();
        try (BufferedReader br =
                new BufferedReader(new FileReader(Helper.PATH_TO_TRANSFERS_IMPORT))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] transferArr = line.split(";");
                String hashFromCSV = transferArr[0];
                if (transferArr.length == 10) {
                    String existingTrackingId = transferArr[9];
                    if (Helper.trackingIdIsValid(existingTrackingId)
                            && transferMap.containsKey(hashFromCSV)) {
                        Transfer transfer = transferMap.get(hashFromCSV);
                        transfer.setExistingTrackingId(existingTrackingId);
                        existingTrackingIds.add(existingTrackingId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return existingTrackingIds;
    }

    public static StringBuilder exportTransfers(ArrayList<Transfer> transfers) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Hash;Pos;Date;Balance;Retoure (Pos);BIC;IBAN;Name;Purpose;Tracking Id\n");
        for (Transfer transfer : transfers) {
            stringBuilder.append(transfer.toCSVString() + "\n");
        }
        return stringBuilder;
    }
}
