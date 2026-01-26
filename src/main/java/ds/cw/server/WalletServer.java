package ds.cw.server;

import ds.cw.zookeeper.LeaderElection;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class WalletServer {

    public static void main(String[] args) throws Exception {

        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 9000;

        // --- Leader Election ---
        LeaderElection election = new LeaderElection();
        election.connect();

        boolean isLeader = election.attemptLeadership();

        if (!isLeader) {
            System.out.println("Follower server – not serving requests");
            Thread.sleep(Long.MAX_VALUE);
        }


        WalletStore store = new WalletStore();
        WalletServiceImpl service = new WalletServiceImpl(store);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .build();

        server.start();
        System.out.println("Wallet Server (LEADER) started on port " + port);

        server.awaitTermination();
    }
}
