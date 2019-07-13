package org.iota.utility.tasks;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.iota.jota.dto.response.FindTransactionResponse;
import org.iota.jota.utils.Parallel;

public class PushMilestones extends Task {

    public static final String COMPASS = 
            "EQSAUZXULTTYZCLNJNTXQTQHOMOFZERHTCGTXOLTVAHKSA9OGAZDEKECURBRIXIJWNPFCQIOVFVVXJVD9DGCJRJTHZ";

    public PushMilestones() {

    }

    @Override
    public void run() {
        boolean stopped = false;
        while (!stopped) {
            FindTransactionResponse transactionHashes = source.getApis().element().findTransactionsByAddresses(COMPASS);
            AtomicInteger totalCount = new AtomicInteger(0);
            try {
                Parallel.of(Arrays.asList(transactionHashes.getHashes()),
                    new Parallel.Operation<String>() {
                        public void perform(String tx) {
                            try {
                                boolean targetHas = source.checkTargetForTransaction(tx);
                                if (!targetHas) {
                                    int newCount = totalCount.incrementAndGet();
                                    if (newCount % 10 == 0) {
                                        System.out.println("Uploaded " + newCount + "milestones!");
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

                System.out.println("Storing done; Added " + totalCount.get() + " milestones");
                System.out.println("Sleeping for 5 minutes... Bye!");
                totalCount.set(0);
                Thread.sleep(1000 * 60  * 5);
            } catch (InterruptedException e) {
                stopped = true;
            }
        }
    }
}
