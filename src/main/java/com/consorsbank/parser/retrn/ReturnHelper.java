package com.consorsbank.parser.retrn;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import com.consorsbank.parser.transfer.Transfer;

public class ReturnHelper {

    public static void findReturnTransfers(ArrayList<Transfer> transfers) {
        outer: for (int i = transfers.size() - 1; i > 0; i--) {
            Transfer transfer = transfers.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Transfer prevTransfer = transfers.get(j);
                long daysBetween = ChronoUnit.DAYS.between(prevTransfer.getLocalDate(),
                        transfer.getLocalDate());
                if (daysBetween <= com.consorsbank.parser.Helper.RETOURE_LIMIT_DAYS
                        && Math.abs(prevTransfer.getReturnBalance()) >= Math
                                .abs(transfer.getBalanceNumber().getValue())) {
                    boolean continueOuter = findReturnTransfer(transfer, prevTransfer, false);
                    if (continueOuter) {
                        continue outer;
                    }
                } else if (daysBetween <= com.consorsbank.parser.Helper.RETOURE_LIMIT_DAYS) {
                    boolean continueOuter = findReturnTransfer(transfer, prevTransfer, true);
                    if (continueOuter) {
                        continue outer;
                    }
                }
            }
        }
    }

    private static boolean findReturnTransfer(Transfer transfer, Transfer prevTransfer,
            boolean findPotentialReturnTransfer) {
        double balanceValue = transfer.getBalanceNumber().getValue();
        double prevBalanceValue = prevTransfer.getBalanceNumber().getValue();

        if (balanceValue > 0 && prevBalanceValue < 0
                && Math.abs(balanceValue - Math
                        .abs(prevBalanceValue)) < com.consorsbank.parser.Helper.EPSILON
                && transfer.getName().equals(prevTransfer.getName())
                && purposeMatches(transfer, prevTransfer)) {
            assignReturnTransfer(transfer, prevTransfer, findPotentialReturnTransfer);
            return true;
        } else if (balanceValue < 0 && prevBalanceValue > 0
                && Math.abs(Math.abs(balanceValue)
                        - prevBalanceValue) < com.consorsbank.parser.Helper.EPSILON
                && transfer.getName().equals(prevTransfer.getName())
                && purposeMatches(transfer, prevTransfer)) {
            // A rare case but it can happen
            assignReturnTransfer(prevTransfer, transfer, findPotentialReturnTransfer);
            return true;
        } else if (balanceValue > 0 && prevBalanceValue < 0
                && (Math.abs(prevBalanceValue)
                        - balanceValue) >= com.consorsbank.parser.Helper.CENT
                && transfer.getName().equals(prevTransfer.getName())
                && purposeMatches(transfer, prevTransfer)) {
            assignReturnTransfer(transfer, prevTransfer, findPotentialReturnTransfer);
            return true;
        } else if (balanceValue > 0 && prevBalanceValue < 0
                && (Math.abs(prevBalanceValue)
                        - balanceValue) >= com.consorsbank.parser.Helper.CENT
                && (transfer.getName().startsWith(prevTransfer.getName())
                        || prevTransfer.getName().startsWith(transfer.getName()))
                && purposeMatches(transfer, prevTransfer)) {
            assignReturnTransfer(transfer, prevTransfer, findPotentialReturnTransfer);
            return true;
        }
        return false;
    }

    private static void assignReturnTransfer(Transfer returnTransfer, Transfer prevTransfer,
            boolean findPotentialReturnTransfer) {
        returnTransfer.setPointToTransfer(prevTransfer, findPotentialReturnTransfer);
    }

    public static boolean purposeMatches(Transfer transfer, Transfer prevTransfer) {
        String purpose = transfer.getPurpose();
        String prevPurpose = prevTransfer.getPurpose();
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
     * Package return transfers other than 1:1, i.e, 1:n, n:1, n:n, and n:m
     * 
     * @param transfers the transfers to find return packagings for
     * 
     * @param transferMap the transfer map to find return packagings for
     */
    public static void packageReturnTransfers(ArrayList<Transfer> transfers,
            LinkedHashMap<String, Transfer> transferMap) {
        HashSet<Transfer> transfersForReturnPackaging =
                findTransfersForReturnPackaging(transferMap);

        for (Transfer transfer : transfersForReturnPackaging) {
            ArrayList<Bin> bins = new ArrayList<Bin>();
            ArrayList<Packet> packets = new ArrayList<Packet>();
            setBinsAndPackets(transfers, transfer, bins, packets);

            if (!(bins.size() == 1 && packets.size() == 1)) {
                // 1:1 packagings work already, treat 1:n, n:1, n:n and n:m packagings instead

                // First, check whether a package fits simply perfect into a bin (for all packets)
                doSimpleBestFitPackaging(bins, packets);
                if (allPacketsPackaged(packets)) {
                    continue;
                }
                // If not, check whether a best fit packaging is possible with a chronological order
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

    private static HashSet<Transfer> findTransfersForReturnPackaging(
            LinkedHashMap<String, Transfer> transferMap) {
        HashSet<Transfer> transfersForReturnPackaging = new HashSet<Transfer>();
        // A retour transfer is a transfer pointing to another transfer
        for (Transfer returnTransfer : transferMap.values()) {
            if (returnTransfer.getPointToTransfer() != null) {
                Transfer anotherTransfer =
                        transferMap.get(returnTransfer.getPointToTransfer().getHash());
                anotherTransfer.getIncomingTransfers().put(returnTransfer.getHash(),
                        returnTransfer);
                transfersForReturnPackaging.add(anotherTransfer);
            }
        }
        return transfersForReturnPackaging;
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

    private static void doSimpleBestFitPackaging(ArrayList<Bin> bins, ArrayList<Packet> packets) {
        for (int i = 0; i < packets.size(); i++) {
            packets.get(i).getTransfer().setPointToTransfer(null, false);
        }

        outer: for (int i = 0; i < bins.size(); i++) {
            double binValue = Math.abs(bins.get(i).getTransfer().getBalanceNumber().getValue());
            for (int j = 0; j < packets.size(); j++) {
                if (packets.get(j).getTransfer().getPointToTransfer() == null) {
                    double packetValue = packets.get(j).getTransfer().getBalanceNumber().getValue();
                    if (binValue == packetValue) {
                        // assign return transfer
                        packets.get(j).getTransfer()
                                .setPointToTransfer(bins.get(i).getTransfer(), false);
                        continue outer;
                    }
                }
            }
        }
    }

    private static void doBestFitPackaging(ArrayList<Bin> bins, ArrayList<Packet> packets,
            boolean timeDriven) {
        for (int i = 0; i < packets.size(); i++) {
            packets.get(i).getTransfer().setPointToTransfer(null, false);
        }

        outer: for (int i = 0; i < bins.size(); i++) {
            double binValue = Math.abs(bins.get(i).getTransfer().getBalanceNumber().getValue());
            for (int j = 0; j < packets.size(); j++) {
                if (packets.get(j).getTransfer().getPointToTransfer() == null) {
                    double packetValue = packets.get(j).getTransfer().getBalanceNumber().getValue();
                    if (binValue >= packetValue) {
                        // assign return transfer
                        packets.get(j).getTransfer()
                                .setPointToTransfer(bins.get(i).getTransfer(), false);
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
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i).getTransfer().getPointToTransfer() == null) {
                return false;
            }
        }
        return true;
    }
}
