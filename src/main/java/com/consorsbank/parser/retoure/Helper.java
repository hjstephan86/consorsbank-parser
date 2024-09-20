package com.consorsbank.parser.retoure;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import com.consorsbank.parser.Transfer;

public class Helper {

    public static void findRetoureTransfers(ArrayList<Transfer> transfers) {
        outer: for (int i = transfers.size() - 1; i > 0; i--) {
            Transfer transfer = transfers.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Transfer prevTransfer = transfers.get(j);
                long daysBetween = ChronoUnit.DAYS.between(prevTransfer.getLocalDate(),
                        transfer.getLocalDate());
                if (daysBetween <= com.consorsbank.parser.Helper.RETOURE_LIMIT_DAYS
                        && !prevTransfer.isBalanced()) {
                    double balanceValue = transfer.getBalanceNumber().getValue();
                    double prevBalanceValue = prevTransfer.getBalanceNumber().getValue();

                    if (balanceValue > 0 && prevBalanceValue < 0
                            && Math.abs(balanceValue - Math
                                    .abs(prevBalanceValue)) < com.consorsbank.parser.Helper.EPSILON
                            && transfer.getName().equals(prevTransfer.getName())) {
                        assignRetoureTransfer(transfer, prevTransfer);
                        prevTransfer.setBalanced(true);
                        continue outer;
                    } else if (balanceValue > 0 && prevBalanceValue < 0
                            && (Math.abs(prevBalanceValue)
                                    - balanceValue) >= com.consorsbank.parser.Helper.CENT
                            && transfer.getName().equals(prevTransfer.getName())
                            && purposeMatches(transfer, prevTransfer)) {
                        assignRetoureTransfer(transfer, prevTransfer);
                        continue outer;
                    } else if (balanceValue > 0 && prevBalanceValue < 0
                            && (Math.abs(prevBalanceValue)
                                    - balanceValue) >= com.consorsbank.parser.Helper.CENT
                            && (transfer.getName().startsWith(prevTransfer.getName())
                                    || prevTransfer.getName().startsWith(transfer.getName()))
                            && purposeMatches(transfer, prevTransfer)) {
                        assignRetoureTransfer(transfer, prevTransfer);
                        continue outer;
                    }
                }
            }
        }
    }

    private static void assignRetoureTransfer(Transfer transfer, Transfer prevTransfer) {
        transfer.setOutgoingRetoureTransfer(prevTransfer);
    }

    public static boolean purposeMatches(Transfer transfer, Transfer otherTransfer) {
        String purpose = transfer.getPurpose();
        String prevPurpose = otherTransfer.getPurpose();
        String[] purposeArr = purpose.split(" ");
        String[] prevPurposeArr = prevPurpose.split(" ");

        if (purposeArr.length > 0 && prevPurposeArr.length > 0) {
            if (transfer.getName().toUpperCase()
                    .contains(com.consorsbank.parser.Helper.CUSTOMER_NAME_AMAZON)) {
                // For Amazon we expect purposes like
                // "305-6835168-8514731 Amazon.de 54M6S" and
                // "305-6835168-8514731 AMZ Amazon.de 5"
                if (purposeArr[0].equals(prevPurposeArr[0]))
                    return true;
            } else if (transfer.getName().toUpperCase()
                    .contains(com.consorsbank.parser.Helper.CUSTOMER_NAME_ZALANDO)) {
                // For Zalando we expect purposes like
                // "10105463479958" and
                // "10105463479958 ZALANDO"
                if (purposeArr[0].equals(prevPurposeArr[0]))
                    return true;
            } else {
                // Allow arbitrary customers, too
                if (purposeArr[0].equals(prevPurposeArr[0]))
                    return true;
            }
        }
        return false;
    }

    /**
     * Package n:m retoure transfers, 1:1 and 1:n retoure packagings work already
     * 
     * @param transfers the transfers to find retoure packagings for
     * 
     * @param transferMap the transfer map to find retoure packagings for
     */
    public static void packageRetoureTransfers(ArrayList<Transfer> transfers,
            LinkedHashMap<String, Transfer> transferMap) {
        HashSet<Transfer> transfersForRetourePackaging =
                findTransfersForRetourePackaging(transferMap);

        for (Transfer transfer : transfersForRetourePackaging) {
            ArrayList<Bin> bins = new ArrayList<Bin>();
            ArrayList<Packet> packets = new ArrayList<Packet>();
            setBinsAndPackets(transfers, transfer, bins, packets);

            if (bins.size() > 1) {
                // 1:n packagings work already, treat n:m packagings instead
                // First, check whether a best fit packaging is possible with a chronological order
                doBestFitPackaging(bins, packets, true);
                if (allPacketsPackaged(packets)) {
                    continue;
                }
                // If not, do a best fit packaging where the bins and packets are ordered by the
                // balance value of the transfer, respectively, in ascending order
                Collections.sort(bins);
                Collections.sort(packets);
                doBestFitPackaging(bins, packets, false);
            }
        }
    }

    private static HashSet<Transfer> findTransfersForRetourePackaging(
            LinkedHashMap<String, Transfer> transferMap) {
        HashSet<Transfer> transfersForRetourePackaging = new HashSet<Transfer>();
        // A retour transfer is an outgoing transfer pointing to an incoming transfer
        for (Transfer outgoingTransfer : transferMap.values()) {
            if (outgoingTransfer.getOutgoingRetoureTransfer() != null) {
                Transfer incomingTransfer =
                        transferMap.get(outgoingTransfer.getOutgoingRetoureTransfer().getHash());
                incomingTransfer.getIncomingTransfers().put(outgoingTransfer.getHash(),
                        outgoingTransfer);
                if (incomingTransfer.getIncomingTransfers().size() > 1) {
                    transfersForRetourePackaging.add(incomingTransfer);
                }
            }
        }
        return transfersForRetourePackaging;
    }

    private static void setBinsAndPackets(ArrayList<Transfer> transfers, Transfer transfer,
            ArrayList<Bin> bins, ArrayList<Packet> packets) {
        // Add transfers (bins) which are part of the order
        // Bins have to have a negative balance value
        for (int i = 0; i < transfers.size(); i++) {
            if (transfers.get(i).getBalanceNumber().getValue() < 0
                    && purposeMatches(transfers.get(i), transfer)) {
                Bin bin = new Bin(transfers.get(i));
                bins.add(bin);
            }
        }
        // Add transfers (packets) which are part of the order
        for (Transfer t : transfer.getIncomingTransfers().values()) {
            Packet packet = new Packet(t);
            packets.add(packet);
        }
    }

    private static void doBestFitPackaging(ArrayList<Bin> bins, ArrayList<Packet> packets,
            boolean timeDriven) {
        for (int i = 0; i < packets.size(); i++) {
            packets.get(i).getTransfer().setOutgoingRetoureTransfer(null);
        }

        outer: for (int i = 0; i < bins.size(); i++) {
            double binValue = Math.abs(bins.get(i).getTransfer().getBalanceNumber().getValue());
            for (int j = 0; j < packets.size(); j++) {
                if (packets.get(j).getTransfer().getOutgoingRetoureTransfer() == null) {
                    double packetValue = packets.get(j).getTransfer().getBalanceNumber().getValue();
                    if (binValue >= packetValue) {
                        // assign retoure transfer
                        packets.get(j).getTransfer()
                                .setOutgoingRetoureTransfer(bins.get(i).getTransfer());
                        binValue -= packetValue;
                    } else {
                        if (timeDriven) {
                            continue outer;
                        }
                    }
                }
            }
        }
    }

    private static boolean allPacketsPackaged(ArrayList<Packet> packets) {
        for (int i = packets.size() - 1; i > 0; i--) {
            if (packets.get(i).getTransfer().getOutgoingRetoureTransfer() == null) {
                return false;
            }
        }
        return true;
    }
}
