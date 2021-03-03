package usage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.NullCallback;

public class Producer {
    
    private Client client;

    void connect(String servers, Client client) throws InterruptedException {
        System.out.println("Connecting to VoltDB...");

        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(server, client);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }
    
    void connectToOneServerWithRetry(String server, Client client) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            }
            catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try { Thread.sleep(sleep); } catch (Exception interruted) {}
                if (sleep < 8000) sleep += sleep;
            }
        }
        System.out.printf("Connected to VoltDB node at: %s.\n", server);
    }
    
    public void runBenchmark(int startGroupId, int endGroupId, int usersPerGroup, int subsPerUser, int countersPerSub) 
            throws InterruptedException, NoConnectionsException, IOException {
        client = ClientFactory.createClient();
        connect("localhost", client);
        
        for(int groupId=startGroupId; groupId<endGroupId; groupId++) {
            for(int userId=groupId*usersPerGroup; userId<(groupId+1)*usersPerGroup; userId++) {
                for(int subId=userId*subsPerUser; subId<(userId+1)*subsPerUser; subId++) {
                    for(int counterId=subId*countersPerSub; counterId<(subId+1)*countersPerSub; counterId++) {
                        client.callProcedure(new NullCallback(), "NewCDR", groupId, userId, subId, counterId);
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException, NoConnectionsException, IOException {
        Producer producer = new Producer();
        int startGroupId = Integer.parseInt(args[0]);
        int endGroupId = Integer.parseInt(args[1]);
        int usersPerGroup = Integer.parseInt(args[2]); // 5
        int subsPerUser = Integer.parseInt(args[3]);   // 2
        int countersPerSub = Integer.parseInt(args[4]);// 10
        
        producer.runBenchmark(startGroupId, endGroupId, usersPerGroup, subsPerUser, countersPerSub);
    }
}
