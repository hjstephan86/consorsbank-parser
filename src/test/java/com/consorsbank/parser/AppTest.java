package com.consorsbank.parser;

import static org.junit.Assert.assertThrows;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.NoSuchElementException;
import org.junit.Test;

public class AppTest {

        private String testFolder = "/home/stephan/Downloads/Kontobewegungen/Test/";
        private String pathToPDFReports = testFolder;
        private String pathToDeliveryReceipts = pathToPDFReports + "Retoure/";
        private String pathToTransfersImport =
                        pathToPDFReports + "Transfers-2024-09-22_10-55-49.csv";
        private String pathToTransfersExport =
                        pathToPDFReports + "Transfers-2024-09-22_10-58-22.csv";

        @Test
        public void testMain() {
                String[] arguments = {pathToPDFReports, pathToDeliveryReceipts};

                String simulatedInput = "1";
                ByteArrayInputStream in =
                                new ByteArrayInputStream(simulatedInput.getBytes());
                System.setIn(in);

                assertThrows(NoSuchElementException.class, () -> {
                        App.main(arguments);
                });
        }

        @Test
        public void testMainParseForExistingTrackingIds() {
                String[] arguments = {pathToPDFReports, pathToDeliveryReceipts,
                                pathToTransfersImport};

                try {
                        String simulatedInput = "g";
                        ByteArrayInputStream in =
                                        new ByteArrayInputStream(simulatedInput.getBytes());
                        System.setIn(in);

                        App.main(arguments);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        @Test
        public void testMainExport() {
                String[] arguments = {pathToPDFReports, pathToDeliveryReceipts,
                                pathToTransfersImport, pathToTransfersExport};
                String pathToReceiptsExport = pathToDeliveryReceipts + ".receipts";

                try {
                        // Check transfers CSV
                        int expectedEntriesCount = 1554;
                        String expectedLastEntry =
                                        "b000b9059601524df6d27601c1e54e16;1554;30.08.2024;500,00;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

                        String simulatedInput = "g";
                        ByteArrayInputStream in =
                                        new ByteArrayInputStream(simulatedInput.getBytes());
                        System.setIn(in);

                        App.main(arguments);

                        File f = new File(pathToTransfersExport);
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

                        // Check receipts CSV
                        expectedEntriesCount = 5;
                        expectedLastEntry =
                                        "3efc5eee5d7e2b1e0f6a1ecd03e19e04e95ee83a13178120e033ca80893c2ca5;Pullover-Boss-TomTailor-orig.pdf;Deutsche Post AG; ;29.09.2024 19:57;233543534973;";

                        f = new File(pathToReceiptsExport);
                        entriesCount = 0;
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
                        e.printStackTrace();
                }
        }
}
