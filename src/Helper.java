public class Helper {

    public static String PATH_TO_PDF_REPORTS = "/home/stephan/Downloads/Kontobewegungen/230583809/";
    public static String PATH_TO_CSV =
            "/home/stephan/Downloads/Kontobewegungen/230583809/Transfers-%DATETIME%.csv";

    public static final String SIMPLE_DATE_FORMAT = "dd.MM.yyyy";
    public static final String REGEX_TRANSFER_TYPES =
            "GEHALT/RENTE|EURO-UEBERW.|LASTSCHRIFT|DAUERAUFTRAG|GIROCARD|GEBUEHREN";
    public static final String KONTOSTAND_ZUM_IN_TXT = "Kontostand zum ";
    public static final String INTERIM_KONTOSTAND_ZUM_IN_TXT = "*** Kontostand zum ";

    public static final int RETOURE_LIMIT_DAYS = 100;

    public static final int POS_COL_WIDTH = 10;
    public static final int DATE_COL_WIDTH = 15;
    public static final int BALANCE_COL_WIDTH = 15;
    public static final int RETOURE_COL_WIDTH = 10;
    public static final int BIC_COL_WIDTH = 15;
    public static final int IBAN_COL_WIDTH = 25;
    public static final int NAME_COL_WIDTH = 25;

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
