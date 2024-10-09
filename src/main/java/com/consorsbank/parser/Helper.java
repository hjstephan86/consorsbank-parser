package com.consorsbank.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.consorsbank.parser.receipt.DeliveryReceipt;
import com.consorsbank.parser.receipt.TrackingIdForReceipt;
import com.mindee.MindeeClient;
import com.mindee.http.Endpoint;
import com.mindee.input.LocalInputSource;
import com.mindee.parsing.common.AsyncPredictResponse;
import com.mindee.product.generated.GeneratedV1;

public class Helper {

    public static String PATH_TO_PDF_REPORTS = "/home/stephan/Downloads/Kontobewegungen/270448588/";
    public static String PATH_TO_DELIVERY_RECEIPTS =
            "/home/stephan/Downloads/Kontobewegungen/Retoure/";
    public static String PATH_TO_TRANSFERS_EXPORT =
            PATH_TO_PDF_REPORTS + "Transfers-%DATETIME%.csv";
    public static String PATH_TO_TRANSFERS_IMPORT =
            PATH_TO_PDF_REPORTS + "Transfers-2024-10-05_19-06-09.csv";
    public static String PATH_TO_DELIVERY_RECEIPTS_FILE_NAME = ".receipts";
    public static String PATH_TO_DELIVERY_RECEIPTS_FILE =
            PATH_TO_DELIVERY_RECEIPTS + PATH_TO_DELIVERY_RECEIPTS_FILE_NAME;

    public static final String SIMPLE_DATE_FORMAT = "dd.MM.yyyy";
    public static final String SIMPLE_DATE_FORMAT_TIME = "yyyy-MM-dd_HH-mm-ss";
    public static final String DATETIME_FORMAT_READ = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_FORMAT_WRITE = "dd.MM.yyyy HH:mm";

    public static final String PDF_REPORT_REGEX_TRANSFER_TYPES =
            "GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN|GUTSCHRIFT";
    public static final String PDF_REPORT_KONTOSTAND_ZUM_IN_TXT = "Kontostand zum ";
    public static final String PDF_REPORT_INTERIM_KONTOSTAND_ZUM_IN_TXT = "*** Kontostand zum ";

    public static final String SHA_ALGORITHM = "SHA-256";
    public static final String CUSTOMER_NAME_AMAZON = "AMAZON";
    public static final String CUSTOMER_NAME_ZALANDO = "ZALANDO";

    public static final String MINDEE_API_KEY = "";
    public static final String MINDEE_API_ENDPOINT_NAME = "trackinglabel";
    public static final String MINDEE_API_ACCOUNT_NAME = "hjstephan86";
    public static final String MINDEE_API_VERSION = "1";

    public static final String DELIVERY_RECEIPT_DHL_SENDER = "Deutsche Post AG";
    public static final String DELIVERY_RECEIPT_HERMES_SENDER = "Hermes AG";
    public static final String DELIVERY_RECEIPT_UPS_SENDER = "United Parcel Service, Inc.";
    public static final String DELIVERY_RECEIPT_RECIPIENT_IN_TXT = ":recipient_name: [{value=";
    public static final String DELIVERY_RECEIPT_SENDER_IN_TXT = ":sender_name: [{value=";
    public static final String DELIVERY_RECEIPT_DATE_IN_TXT = ":shipment_date: [{value=";
    public static final String DELIVERY_RECEIPT_TIME_IN_TXT = ":shipment_time: [{value=";
    public static final String DELIVERY_RECEIPT_TRACKING_ID_IN_TXT = ":tracking_number: [{value=";
    public static final String DELIVERY_RECEIPT_ID_IN_TXT = ":id: [{value=";

    public static final double EPSILON = 1e-9;
    public static final double CENT = 0.01;

    public static final int RETOURE_LIMIT_DAYS = 100;

    public static final int POS_COL_WIDTH = 10;
    public static final int DATE_COL_WIDTH = 15;
    public static final int BALANCE_COL_WIDTH = 15;
    public static final int RETOURE_COL_WIDTH = 15;
    public static final int BIC_COL_WIDTH = 15;
    public static final int IBAN_COL_WIDTH = 25;
    public static final int NAME_COL_WIDTH = 25;

    public static final int EMPTY_COL_WIDTH = 5;
    public static final int SENDER_COL_WIDTH = 22;
    public static final int RECEPIENT_COL_WIDTH = 22;
    public static final int DATETIME_COL_WIDTH = 22;
    public static final int TRACKING_ID_COL_WIDTH = 22;
    public static final int FILENAME_COL_WIDTH = 22;

    public static final int TRUNCATE_COL_WIDTH_DELTA = 5;

    public static final String CONSOLE_COLOR_YELLOW = "\033[0;33m";
    public static final String CONSOLE_COLOR_BLUE = "\033[0;34m";
    public static final String CONSOLE_COLOR_CYAN = "\033[0;36m";
    public static final String CONSOLE_COLOR_RESET = "\033[0m";
    public static final String CONSOLE_COLOR_RED = "\033[0;31m";
    public static final String CONSOLE_COLOR_GREEN = "\033[0;32m";
    public static final String CONSOLE_COLOR_GRAY = "\033[90m";

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String truncate(String str, int length) {
        return (str.length() > length)
                ? str.substring(0, length) + "..."
                : str;
    }

    public static String getPDFReportText(String filename) throws IOException {
        PDDocument pdDocument = Loader.loadPDF(new File(filename));
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        return pdfTextStripper.getText(pdDocument);
    }

    public static String getDeliveryReceiptText(String filename)
            throws IOException, InterruptedException {
        MindeeClient mindeeClient = new MindeeClient(Helper.MINDEE_API_KEY);
        LocalInputSource inputSource = new LocalInputSource(new File(filename));
        Endpoint endpoint = new Endpoint(
                Helper.MINDEE_API_ENDPOINT_NAME,
                Helper.MINDEE_API_ACCOUNT_NAME,
                Helper.MINDEE_API_VERSION);

        AsyncPredictResponse<GeneratedV1> response = mindeeClient.enqueueAndParse(
                GeneratedV1.class,
                endpoint,
                inputSource);

        return response.toString();
    }

    public static boolean isTrackingIdValid(String trackingId) {
        return isDHLTrackingId(trackingId) ^ isHermesTrackingId(trackingId)
                ^ isUPSTrackingId(trackingId);
    }

    public static boolean isDHLTrackingId(String trackingId) {
        // DHL: start optionally with "JD" or "JJD" followed by 10 to 20 digits
        String DHLRegex = "^(JD|JJD)?[0-9]{10,20}$";
        return trackingId.matches(DHLRegex);
    }

    public static boolean isHermesTrackingId(String trackingId) {
        // Hermes: start with "H" followed by 19 digits
        String HermesRegex = "H\\d{19}";
        return trackingId.matches(HermesRegex);
    }

    public static boolean isUPSTrackingId(String trackingId) {
        // UPS: start with E followed by 13 digits, e.g., E4016129636432
        String HermesRegex = "E\\d{13}";
        return trackingId.matches(HermesRegex);
    }

    public static TrackingIdForReceipt getTrackingIdForReceipt(List<DeliveryReceipt> receipts,
            int number, HashSet<String> existingTrackingIds) {
        if (receipts.size() == 0)
            return null;
        if (number > 0) {
            int counter = 1;
            for (DeliveryReceipt receipt : receipts) {
                for (String trackingId : receipt.getTrackingIds()) {
                    if (!existingTrackingIds.contains(trackingId)) {
                        if (counter == number) {
                            TrackingIdForReceipt trackingIdForReceipt =
                                    new TrackingIdForReceipt(trackingId, receipt);
                            return trackingIdForReceipt;
                        }
                        counter++;
                    }
                }
            }
        }
        return null;
    }

    public static String getHash(String input) {
        StringBuilder hexString = new StringBuilder();
        try {
            // Create a MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Hash the input string
            byte[] hashBytes = md.digest(input.getBytes());

            // Convert the byte array to a hex string
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    public static String getFileHash(String algorithm, String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            FileInputStream fis = new FileInputStream(filePath);
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
