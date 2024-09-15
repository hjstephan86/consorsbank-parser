package com.consorsbank.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.mindee.MindeeClient;
import com.mindee.http.Endpoint;
import com.mindee.input.LocalInputSource;
import com.mindee.parsing.common.AsyncPredictResponse;
import com.mindee.product.generated.GeneratedV1;

public class Helper {

    public static String MINDEE_API_KEY = "";

    public static String PATH_TO_PDF_REPORTS = "/home/stephan/Downloads/Kontobewegungen/230583809/";
    public static String PATH_TO_DELIVERY_RECEIPTS =
            "/home/stephan/Downloads/Kontobewegungen/Retoure/";
    public static String PATH_TO_CSV =
            "/home/stephan/Downloads/Kontobewegungen/230583809/Transfers-%DATETIME%.csv";

    public static final String SIMPLE_DATE_FORMAT = "dd.MM.yyyy";
    public static final String DATETIME_FORMAT_READ = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_FORMAT_WRITE = "dd.MM.yyyy HH:mm";

    public static final String PDF_REPORT_REGEX_TRANSFER_TYPES =
            "GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN";
    public static final String PDF_REPORT_KONTOSTAND_ZUM_IN_TXT = "Kontostand zum ";
    public static final String PDF_REPORT_INTERIM_KONTOSTAND_ZUM_IN_TXT = "*** Kontostand zum ";

    public static final String DELIVERY_RECEIPT_DEFAULT_SENDER = "Deutsche Post AG";
    public static final String DELIVERY_RECEIPT_RECIPIENT_IN_TXT = ":recipient_name: [{value=";
    public static final String DELIVERY_RECEIPT_SENDER_IN_TXT = ":sender_name: [{value=";
    public static final String DELIVERY_RECEIPT_DATE_IN_TXT = ":shipment_date: [{value=";
    public static final String DELIVERY_RECEIPT_TIME_IN_TXT = ":shipment_time: [{value=";
    public static final String DELIVERY_RECEIPT_TRACKING_ID_IN_TXT = ":tracking_number: [{value=";

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
                "trackinglabel",
                "hjstephan86",
                "1");

        AsyncPredictResponse<GeneratedV1> response = mindeeClient.enqueueAndParse(
                GeneratedV1.class,
                endpoint,
                inputSource);

        return response.toString();
    }

    public static boolean bankIDExists(String bankID) {
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

    public static boolean trackingIDExists(String trackingID) {
        // 20 to 39 characters
        String regex = "\\b(\\d{10,39})\\b";

        // Pattern und Matcher verwenden
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(trackingID);

        return matcher.matches();
    }

    public static DeliveryReceipt getDeliveryReceipt(ArrayList<DeliveryReceipt> receipts,
            int number) {
        if (receipts.size() == 0)
            return null;
        if (number > 0 && number <= receipts.size()) {
            int counter = 1;
            for (DeliveryReceipt receipt : receipts) {
                if (counter == number) {
                    return receipt;
                }
                counter++;
            }
        }
        return null;
    }
}
