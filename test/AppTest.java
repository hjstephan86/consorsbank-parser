import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.junit.Test;

public class AppTest {

    @Test
    public void testMain() {
        int expectedEntriesCount = 1554;
        String expectedLastEntry =
                "1554;30.08.2024;500,00;0;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe";

        String pathToPDFs = "/home/stephan/Downloads/Kontobewegungen/Test/";
        String pathToCSV =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-08-15_17-27-53.csv";

        String[] arr = {pathToPDFs, pathToCSV};
        try {
            App.main(arr);

            File f = new File(pathToCSV);
            String lastEntry = null;
            int entriesCount = -1; // Skip the headline
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    entriesCount++;
                    lastEntry = line;
                }
            }

            assert (lastEntry != null && lastEntry.equals(expectedLastEntry));
            assert (entriesCount == expectedEntriesCount);

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }
}
