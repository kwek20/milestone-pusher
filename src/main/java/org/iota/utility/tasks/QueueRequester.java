package org.iota.utility.tasks;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.iota.jota.dto.response.IotaCustomResponse;
import org.iota.jota.utils.Parallel;

public class QueueRequester extends Task {

    private static String CMD = "transactionRequester.getRequest";

    public QueueRequester() {

    }

    @Override
    public void run() {
        boolean stopped = false;
        while (!stopped) {
            Collection<String> transactionHashes = null;

            try {
                IotaCustomResponse response = source.getTarget().callIxi(CMD);
                transactionHashes = ((Map) response.getArg("transactions")).values();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("--------------------------------------");
                System.out.println("Check if this node has the TX requester IXI!");
                System.out.println(
                        "If you need it, it is located in the jar -> src/main/resources/transactionRequester.");
                System.out.println("The correct command we attempt to run is \"" + CMD + "\"");
                System.exit(0);
            }

            AtomicInteger totalCount = new AtomicInteger(0);
            try {
                Parallel.of(transactionHashes, new Parallel.Operation<String>() {
                    public void perform(String tx) {
                        try {
                            boolean targetHas = source.checkTargetForTransaction(tx);
                            if (!targetHas) {
                                int newCount = totalCount.incrementAndGet();
                                if (newCount % 10 == 0) {
                                    System.out.println("Uploaded " + newCount + " new transactions!");
                                }

                                String trytes = source.getTransactionTrytes(tx);
                                if (trytes == null) {
                                    System.out.println("WARN: Source nodes did not contain " + tx + ".. Skipping!");
                                    return;
                                }

                                source.getTarget().storeTransactions(trytes);
                            }
                        } catch (Exception e) {
                            System.out.println("Encountered error during handling of " + tx);
                            e.printStackTrace();
                        }
                    }
                });

                System.out.println("Storing done; Added " + totalCount.get() + " missing transactions");
                System.out.println("Sleeping for 10 seconds... Bye!");
                totalCount.set(0);
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                stopped = true;
            }
        }
    }
}
