package com.consorsbank.parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import org.junit.Test;

public class AppTest {

    @Test
    public void testMain() {
        String pathToPDFReports = "/home/stephan/Downloads/Kontobewegungen/Test/";
        String pathToDeliveryReceipts = "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/";
        String pathToTransfersExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToTransfersImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToReceiptsExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-%DATETIME%.csv";
        String pathToReceiptsImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-2024-09-23_16-07-12.csv";
        String[] arguments = {pathToPDFReports, pathToDeliveryReceipts, pathToTransfersExport,
                pathToTransfersImport, pathToReceiptsExport, pathToReceiptsImport};

        int expectedEntriesCount = 1554;
        String expectedLastEntry =
                "b000b9059601524df6d27601c1e54e16;1554;30.08.2024;500;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

        try {
            String simulatedInput = "1";
            ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes());
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMainGenerate() {
        String pathToPDFReports = "/home/stephan/Downloads/Kontobewegungen/Test/";
        String pathToDeliveryReceipts = "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/";
        String pathToTransfersExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToTransfersImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToReceiptsExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-%DATETIME%.csv";
        String pathToReceiptsImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-2024-09-23_16-07-12.csv";
        String[] arguments = {pathToPDFReports, pathToDeliveryReceipts, pathToTransfersExport,
                pathToTransfersImport, pathToReceiptsExport, pathToReceiptsImport};

        int expectedEntriesCount = 1554;
        String expectedLastEntry =
                "b000b9059601524df6d27601c1e54e16;1554;30.08.2024;500;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

        try {
            String simulatedInput = "g";
            ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes());
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMainParseForExistingTrackingIds() {
        String pathToPDFReports = "/home/stephan/Downloads/Kontobewegungen/Test/";
        String pathToDeliveryReceipts = "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/";
        String pathToTransfersExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToTransfersImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToReceiptsExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-%DATETIME%.csv";
        String pathToReceiptsImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-2024-09-23_16-07-12.csv";
        String[] arguments = {pathToPDFReports, pathToDeliveryReceipts, pathToTransfersExport,
                pathToTransfersImport, pathToReceiptsExport, pathToReceiptsImport};

        int expectedEntriesCount = 1554;
        String expectedLastEntry =
                "b000b9059601524df6d27601c1e54e16;1554;30.08.2024;500;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

        try {
            String simulatedInput = "g";
            ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes());
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMainRemoveExistingDeliveryReceipts() {
        String pathToPDFReports = "/home/stephan/Downloads/Kontobewegungen/Test/";
        String pathToDeliveryReceipts = "/home/stephan/Downloads/Kontobewegungen/Test/Retoure/";
        String pathToTransfersExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToTransfersImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Transfers-2024-09-22_10-55-49.csv";
        String pathToReceiptsExport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-%DATETIME%.csv";
        String pathToReceiptsImport =
                "/home/stephan/Downloads/Kontobewegungen/Test/Receipts-2024-09-23_16-07-12.csv";
        String[] arguments = {pathToPDFReports, pathToDeliveryReceipts, pathToTransfersExport,
                pathToTransfersImport, pathToReceiptsExport, pathToReceiptsImport};

        int expectedEntriesCount = 1554;
        String expectedLastEntry =
                "b000b9059601524df6d27601c1e54e16;1554;30.08.2024;500;;<SPBIDE3B>;DE72480501610150252765;DAMARIS EPP;Erbe;";

        try {
            String simulatedInput = "g";
            ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes());
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
