package org.iota.utility;

import java.net.URL;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.iota.jota.IotaAPI;
import org.iota.jota.dto.response.FindTransactionResponse;
import org.iota.jota.dto.response.GetTrytesResponse;
import org.iota.jota.utils.Parallel;

public class PushMilestones {
    
    public static final String COMPASS = 
            "EQSAUZXULTTYZCLNJNTXQTQHOMOFZERHTCGTXOLTVAHKSA9OGAZDEKECURBRIXIJWNPFCQIOVFVVXJVD9DGCJRJTHZ";
    private static final String EMPTY_TX = 
            "999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999";
    
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println(Arrays.toString(args));
                System.out.println("Usage: java -jar pushmilestones.jar [target-node] [source-node-1] [source-node-x]");
                System.out.println("Milestones will be queried from source nodes and send to target node");
                return;
            }
            
            String target = args[0];
            if (!ArgValidator.isUrl(target)) {
                System.out.println("target node must be a valid url/ip/address");
                return;
            }
            
            URL[] sources = new URL[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                if (!ArgValidator.isUrl(args[i])) {
                    System.out.println("source node " + args[i] + " is not a valid url/ip/address");
                    return;
                }
                sources[i-1] = new URL(args[i]);
            }
            
            new PushMilestones(new URL(target), sources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private IotaAPI toUpdate;
    private Queue<IotaAPI> apis;
    
    public PushMilestones(URL target, URL[] sources) {
        toUpdate = makeApi(target);
        
        apis = new ConcurrentLinkedQueue<IotaAPI>();
        for (URL url : sources) {
            apis.add(makeApi(url));
        }
        
        try {
            toUpdate.getNodeInfo();
        } catch (Exception e) {
            System.out.println("Failed to connect to target node");
            throw e;
        }
        
        // Then test all targets for at least 1 working
        if (testNodesOnline()) {
            System.out.println("Source and target nodes available.. Starting! Kill program to interrupt.");
            syncNode();
        } else {
            System.out.println("No source node online.. Shutting down.");
            System.exit(0);
        }
    }

    private void syncNode() {
        boolean stopped = false;
        while (!stopped) {
            FindTransactionResponse transactionHashes = apis.element().findTransactionsByAddresses(COMPASS);
            AtomicInteger totalCount = new AtomicInteger(0);
            try {
                Parallel.of(Arrays.asList(transactionHashes.getHashes()),
                    new Parallel.Operation<String>() {
                        public void perform(String tx) {
                            try {
                                boolean targetHas = checkTargetForTransaction(tx);
                                if (!targetHas) {
                                    int newCount = totalCount.incrementAndGet();
                                    if (newCount % 10 == 0) {
                                        System.out.println("Uploaded " + newCount + "milestones!");
                                    }
                                    
                                    String trytes = null;
                                    for (IotaAPI api : apis) {
                                        GetTrytesResponse trytesRes = api.getTrytes(tx);
                                        if (trytesRes.getTrytes().length != 0 && 
                                                !trytesRes.getTrytes()[0].equals(EMPTY_TX)) {
                                            trytes = trytesRes.getTrytes()[0];
                                        }
                                    }
                                    
                                    if (trytes == null) {
                                        System.out.println("WARN: Source nodes did not contain " + tx + ".. Skipping!");
                                        return;
                                    }
                                    
                                    toUpdate.storeTransactions(trytes);
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

    protected boolean checkTargetForTransaction(String tx) {
        GetTrytesResponse trytes = toUpdate.getTrytes(tx);
        if (trytes.getTrytes().length == 0) {
            return false;
        }
        
        return !trytes.getTrytes()[0].equals(EMPTY_TX);
    }

    private boolean testNodesOnline() {
        AtomicInteger numSuccess = new AtomicInteger(0);
        try {
            Parallel.of(apis,
                new Parallel.Operation<IotaAPI>() {
                    public void perform(IotaAPI api) {
                        try {
                            api.getNodeInfo();
                            numSuccess.incrementAndGet();
                        } catch (Exception e) {
                            System.out.println("Failed to connect API to " + api.getHost());
                            apis.remove(api);
                        }
                    }
                });
        } catch (InterruptedException e) {
            return false;
        }
        
        return numSuccess.get() > 0;
    }

    private static IotaAPI makeApi(URL target) {
        
        return new IotaAPI.Builder()
                .port(target.getPort())
                .host(target.getHost())
                .protocol(target.getProtocol())
                .timeout(5)
                .build();
    }
}
