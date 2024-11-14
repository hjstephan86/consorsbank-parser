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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(!t.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // After packaging we still expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(t.isPackaged());
                assertTrue(x.isPackaged());
        }

        @Test
        public void testPackageReturnTransfers1To1FitTopDown() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 14.5, 10, '+',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);

                Transfer x = createTransfer("Amazon", 14.5, 14, '-',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);

                // Here, we expect no 1:1 return assignment
                assertTrue(x.getPointToTransfer() == null);
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(!t.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // After packaging we still expect no 1:1 return assignment
                assertTrue(x.getPointToTransfer() == null);
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(!t.isPackaged());
                assertTrue(!x.isPackaged());

                transfers = new ArrayList<Transfer>();
                // Now change the date of x
                t = createTransfer("Amazon", 14.5, 11, '+',
                                "305-1103929-6143535 AMZN Mktp DE IW");
                transfers.add(t);
                x = createTransfer("Amazon", 14.5, 14, '-',
                                "305-1103929-6143535 AMZN Mktp DE WU");
                transfers.add(x);

                transferMap = getTransferMap(transfers);
                ReturnHelper.findReturnTransfers(transfers);

                // Here, we expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer() == null);
                assertTrue(t.getPointToTransfer().equals(x));
                assertTrue(!t.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // After packaging we still expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer() == null);
                assertTrue(t.getPointToTransfer().equals(x));
                assertTrue(t.isPackaged());
                assertTrue(x.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(!t.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);
                // After packaging we still expect a 1:1 return assignment
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(t.isPackaged());
                assertTrue(x.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());
                assertTrue(!y.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a n:n return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
                assertTrue(y.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());
                assertTrue(!y.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a n:n return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
                assertTrue(y.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a simple perfect packaging
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we still expect the same packaging as before
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());
                assertTrue(!y.isPackaged());
                assertTrue(!z.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a n:m return assignment with a chronological
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
                assertTrue(y.isPackaged());
                assertTrue(z.isPackaged());
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
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());
                assertTrue(!y.isPackaged());
                assertTrue(!z.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a n:m return assignment with a best fit
                // ordering
                assertTrue(x.getPointToTransfer().equals(t));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
                assertTrue(y.isPackaged());
                assertTrue(z.isPackaged());
        }

        @Test
        public void testPackageReturnTransfersNToMNotFitPackY() {
                ArrayList<Transfer> transfers = new ArrayList<Transfer>();

                Transfer t = createTransfer("Amazon", 19.93, 11, '-',
                                "303-9119904-0875516 Amazon.de 5MPCJ");
                transfers.add(t);

                Transfer u = createTransfer("Amazon", 37.96, 11, '-',
                                "303-9119904-0875516 Amazon.de 6PGVD");
                transfers.add(u);

                Transfer x = createTransfer("Amazon", 18.98, 14, '+',
                                "303-9119904-0875516 Amazon.de 10ID2");
                transfers.add(x);

                Transfer y = createTransfer("Amazon", 19.93, 14, '+',
                                "303-9119904-0875516 Amazon.de 5MBA3");
                transfers.add(y);

                Transfer z = createTransfer("Amazon", 18.98, 30, '+',
                                "303-9119904-0875516 Amazon.de 3KAJS");
                transfers.add(z);

                LinkedHashMap<String, Transfer> transferMap = getTransferMap(transfers);

                ReturnHelper.findReturnTransfers(transfers);

                // Here, we expect a 1:n return assignment
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(u));
                assertTrue(z.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(!t.isPackaged());
                assertTrue(!u.isPackaged());
                assertTrue(!x.isPackaged());
                assertTrue(!y.isPackaged());
                assertTrue(!z.isPackaged());

                ReturnHelper.packageReturnTransfers(transfers,
                                transferMap);

                // However, after packaging we expect a n:m return assignment with a best fit
                // ordering
                assertTrue(x.getPointToTransfer().equals(u));
                assertTrue(y.getPointToTransfer().equals(t));
                assertTrue(z.getPointToTransfer().equals(u));
                assertTrue(t.getPointToTransfer() == null);
                assertTrue(u.getPointToTransfer() == null);

                assertTrue(t.isPackaged());
                assertTrue(u.isPackaged());
                assertTrue(x.isPackaged());
                assertTrue(y.isPackaged());
                assertTrue(z.isPackaged());
        }
}
