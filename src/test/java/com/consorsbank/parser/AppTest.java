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
        private String pathToDeliveryReceipts = pathToPDFReports + "Return/";
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
                        int expectedEntriesCount = 1578;
                        String expectedLastEntry =
                                        "b000b9059601524df6d27601c1e54e16;1578;30.08.2024;500,00;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

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
                        expectedEntriesCount = 6;
                        expectedLastEntry =
                                        "14d3443920139acedd7ace1e3a304e1fc61b69434996b549d6cd7992702a0a28;Scanned_20241002-1747-Nike-43.jpg;Hermes AG; ;02.10.2024 17:34;H1001990069063401019;";

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
