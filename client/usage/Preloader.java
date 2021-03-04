package usage;

import java.util.concurrent.CountDownLatch;

import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NullCallback;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;
import org.voltdb.types.TimestampType;

public class Preloader {

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
    
    public class FailureClassback implements BulkLoaderFailureCallBack {

        @Override
        public void failureCallback(Object arg0, Object[] arg1, ClientResponse arg2) {
            System.out.println("load failed " + arg0 + arg2.getStatusString() + "\n");
            for (int i=0; i<arg1.length; i++) {
                System.out.println(arg1[i]);
            }
        }
        
    }
    
    public void load(String server, int groups, int usersPerGroup, int subsPerUser) throws Exception {
        client = ClientFactory.createClient();
        connect(server, client);
        BulkLoaderFailureCallBack failureCallback = new FailureClassback();
        VoltBulkLoader groupLoader = client.getNewBulkLoader("user_groups", 1000, failureCallback);
        VoltBulkLoader userLoader = client.getNewBulkLoader("user_account", 10000, failureCallback);
        VoltBulkLoader subLoader = client.getNewBulkLoader("subscriptions", 100000, failureCallback);

        for(int groupId=0; groupId<groups; groupId++) {
            TimestampType now = new TimestampType();
            groupLoader.insertRow("group", groupId, "a", "b", 5, 10, now);
            for(int userId=groupId*usersPerGroup; userId<(groupId+1)*usersPerGroup; userId++) {
                userLoader.insertRow("user", userId, "", 0, "", groupId, now);
                for(int subId=userId*subsPerUser; subId<(userId+1)*subsPerUser; subId++) {
                    subLoader.insertRow("subscriptions", subId, userId, now, now, now, "", now);
                }
            }
        }
        groupLoader.drain();
        userLoader.drain();
        subLoader.drain();
    }
    
    public static void main(String[] args) throws Exception {
        String server = args[0];
        int groups = Integer.parseInt(args[1]);
        int usersPerGroup = Integer.parseInt(args[2]);
        int subsPerUser = Integer.parseInt(args[3]);
        
        Preloader preloader = new Preloader();
        preloader.load(server, groups, usersPerGroup, subsPerUser);
    }
}
