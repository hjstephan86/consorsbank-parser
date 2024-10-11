package com.consorsbank.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import org.junit.Test;
import com.consorsbank.parser.retrn.ReturnHelper;
import com.consorsbank.parser.transfer.BalanceNumber;
import com.consorsbank.parser.transfer.Transfer;

public class ReturnHelperTest {

        private Transfer createTransfer(String name, double value, int day, char sign,
                        String purpose) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(2024, Calendar.DECEMBER, day, 0, 0, 0);

                Date date = calendar.getTime();
                Number number = value;
                BalanceNumber balanceNumber = new BalanceNumber(number, sign);
                Transfer t = new Transfer(balanceNumber, date);
                t.setName(name);
                t.setPurpose(purpose);

                return t;
        }

        private LinkedHashMap<String, Transfer> getTransferMap(ArrayList<Transfer> transfers) {
                LinkedHashMap<String, Transfer> transferMap = new LinkedHashMap<String, Transfer>();
                for (int i = 0; i < transfers.size(); i++) {
                        Transfer transfer = transfers.get(i);
                        transfer.setPosition(i + 1);
                        transfer.generateHash();
                        transferMap.put(transfer.getHash(), transfer);
                }
                return transferMap;
        }

        @Test
        public void testPurposeMatches() {
                Transfer t = createTransfer("Amazon", 10, 10, '-',
                                "302-8845287-5188355 Amazon.de SSROA");
                Transfer u = createTransfer("Amazon", 10, 11, '+',
                                "302-8845287-5188355 AMZ Amazon.de 1");

                assertTrue(ReturnHelper.purposeMatches(t, u));

                t.setPurpose("302-8845287-5188357 AMZ Amazon.de 1");
                assertFalse(ReturnHelper.purposeMatches(t, u));

                u.setPurpose("305-1103929-6143535 AMZN Mktp DE 3E");
                t.setPurpose("303-8151108-6247541 AMZN Mktp DE 48");
                assertFalse(ReturnHelper.purposeMatches(t, u));
        }

        @Test
        public void testPackageReturnTransfers1To1Fit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer x = createTransfer("Amazon", 14.5, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // After packaging we still expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));
        }

        @Test
        public void testPackageReturnTransfers1To1NotFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer x = createTransfer("Amazon", 10.5, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // After packaging we still expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));
        }

        @Test
        public void testPackageReturnTransfersNToNNotFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u = createTransfer("Amazon", 21.0, 12, '-',
                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x = createTransfer("Amazon", 10.5, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                Transfer y = createTransfer("Amazon", 5.5, 15, '+',
                                "305-1103929-6143535 AMZN Mktp DE LS");
                transfers.add(y);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:n return assignment
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we expect a n:n return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
        }

        @Test
        public void testPackageReturnTransfersNToNFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u = createTransfer("Amazon", 21.0, 12, '-',
                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x = createTransfer("Amazon", 21.0, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                Transfer y = createTransfer("Amazon", 14.5, 15, '+',
                                "305-1103929-6143535 AMZN Mktp DE LS");
                transfers.add(y);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:n return assignment
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we expect a n:n return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(t));
        }


        @Test
        public void testPackageReturnTransfersNTo1Fit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t =
                                createTransfer("Amazon", 11.16, 11, '-',
                                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u =
                                createTransfer("Amazon", 12.99, 12, '-',
                                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x =
                                createTransfer("Amazon", 11.16, 14, '+',
                                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect that x is simply packaged to u (as already done)
                assertTrue(x.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we expect a simple perfect packaging
                assertTrue(x.getPointToTransfer().equals(t));
        }

        @Test
        public void testPackageReturnTransfersNTo1NotFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t =
                                createTransfer("Amazon", 11.16, 11, '-',
                                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u =
                                createTransfer("Amazon", 12.99, 12, '-',
                                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x =
                                createTransfer("Amazon", 10.16, 14, '+',
                                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect that x is simply packaged to u (as already done)
                assertTrue(x.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we still expect the same packaging as before
                assertTrue(x.getPointToTransfer().equals(t));
        }

        @Test
        public void testPackageReturnTransfersNToMNotFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u = createTransfer("Amazon", 21.0, 12, '-',
                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x = createTransfer("Amazon", 10.5, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                Transfer y = createTransfer("Amazon", 5.5, 15, '+',
                                "305-1103929-6143535 AMZN Mktp DE LS");
                transfers.add(y);

                Transfer z = createTransfer("Amazon", 5.5, 16, '+',
                                "305-1103929-6143535 AMZN Mktp DE LS");
                transfers.add(z);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:n return assignment
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we expect a n:m return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));
        }

        @Test
        public void testPackageReturnTransfersNToMNotFitBestFit() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 8.5, 11, '-',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer u = createTransfer("Amazon", 21.0, 12, '-',
                                "305-1103929-6143535 AMZN Mktp DE OM");
                transfers.add(u);

                Transfer x = createTransfer("Amazon", 10.5, 13, '+',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                Transfer y = createTransfer("Amazon", 5.5, 15, '+',
                                "305-1103929-6143535 AMZN Mktp DE LS");
                transfers.add(y);

                Transfer z = createTransfer("Amazon", 5.5, 14, '+',
                                "305-1103929-6143535 AMZN Mktp DE PT");
                transfers.add(z);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);
                // Here, we expect a 1:n return assignment
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // However, after packaging we expect a n:m return assignment with a best fit
                // ordering
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(t));
                assertTrue(z.getPointToTransfer().equals(u));
        }
}
