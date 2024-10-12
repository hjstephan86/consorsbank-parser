package com.consorsbank.parser;

import static org.junit.Assert.assertThrows;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public void testMainExpectException() {
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
    public void testMainExport() {
        String[] arguments = {pathToPDFReports, pathToDeliveryReceipts,
                pathToTransfersImport, pathToTransfersExport};
        String pathToReceiptsExport = pathToDeliveryReceipts + ".receipts";

        try {
            // Check transfers CSV
            int expectedEntriesCount = 1119;
            String expectedLastEntry =
                    "f3bb94d1d864c37bdf51d6fcbf3f9514;1119;31.07.2024;2505,15;;<WELADEDD>;DE51300500000004006615;Landeshauptkasse Nordrhein-Westfalen fuer LBV;E190516 4-09136806-Bezuege 08/2024;";
            LinkedHashMap<Integer, String> expMapPosToReturnPos = getExpMapPosToReturnPos();
            LinkedHashMap<Integer, String> expMapPosToTrackingIds = getExpMapPosToTrackingIds();

            String simulatedInput = "g";
            ByteArrayInputStream in =
                    new ByteArrayInputStream(simulatedInput.getBytes());
            System.setIn(in);

            App.main(arguments);

            LinkedHashMap<Integer, String> mapPosToReturnPos = new LinkedHashMap<Integer, String>();
            LinkedHashMap<Integer, String> mapPosToTrackingIds =
                    new LinkedHashMap<Integer, String>();
            File f = new File(pathToTransfersExport);
            String lastEntry = null;
            int entriesCount = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("Hash")) {
                        String returnPos = parseReturnPosition(line);
                        if (returnPos != null && returnPos.length() > 0) {
                            mapPosToReturnPos.put((entriesCount + 1), returnPos);
                        }
                        String trackingId = parseTrackingId(line);
                        if (trackingId != null && trackingId.length() > 0) {
                            mapPosToTrackingIds.put((entriesCount + 1), trackingId);
                        }
                        entriesCount++;
                        lastEntry = line;
                    }
                }
            }

            assert (lastEntry != null && lastEntry.equals(expectedLastEntry));
            assert (entriesCount == expectedEntriesCount);
            assert (expMapPosToReturnPos.size() == mapPosToReturnPos.size());
            for (Map.Entry<Integer, String> entry : expMapPosToReturnPos.entrySet()) {
                assert (mapPosToReturnPos.get(entry.getKey()).equals(entry.getValue()));
            }
            assert (expMapPosToTrackingIds.size() == mapPosToTrackingIds.size());
            for (Map.Entry<Integer, String> entry : expMapPosToTrackingIds.entrySet()) {
                assert (expMapPosToTrackingIds.get(entry.getKey()).equals(entry.getValue()));
            }

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

    private LinkedHashMap<Integer, String> getExpMapPosToReturnPos() {
        LinkedHashMap<Integer, String> expMapPosToReturnPos =
                new LinkedHashMap<Integer, String>() {
                    {
                        put(59, "33");
                        put(61, "33");
                        put(66, "54");
                        put(102, "54");
                        put(370, "350");
                        put(420, "56");
                        put(474, "455");
                        put(477, "350");
                        put(593, "583");
                        put(594, "579");
                        put(595, "584");
                        put(596, "584");
                        put(597, "587");
                        put(625, "586");
                        put(713, "701");
                        put(740, "728");
                        put(866, "860");
                        put(953, "939");
                        put(973, "962");
                        put(983, "938");
                        put(1046, "999");
                        put(1062, "1056");
                        put(1078, "995");
                        put(1109, "1110");
                    }
                };
        return expMapPosToReturnPos;
    }

    private LinkedHashMap<Integer, String> getExpMapPosToTrackingIds() {
        LinkedHashMap<Integer, String> expMapPosToTrackingIds =
                new LinkedHashMap<Integer, String>() {
                    {
                        put(59, "233492001037");
                        put(61, "233492001037");
                    }
                };
        return expMapPosToTrackingIds;
    }

    private String parseTrackingId(String line) {
        try {
            String trackingId = null;
            String[] lineArr = line.split(";");
            if (lineArr.length == 10) {
                trackingId = lineArr[9];
            }
            return trackingId;
        } catch (Exception e) {
            return null;
        }
    }

    private String parseReturnPosition(String line) {
        try {
            String returnPosition = null;
            String[] lineArr = line.split(";");
            returnPosition = lineArr[4];
            return returnPosition;
        } catch (Exception e) {
            return null;
        }
    }
}
