package ds.cw.server;

import ds.cw.zookeeper.LeaderElection;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class WalletServer {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Usage: WalletServer <port> <shardId>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String shardId = args[1];

        // --- Leader Election ---
        LeaderElection election = new LeaderElection(shardId);
        election.connect();

        System.out.println("Shard " + shardId + ": Participating in leader election...");
        election.awaitLeadership();

        WalletStore store = new WalletStore();
        WalletServiceImpl service = new WalletServiceImpl(store, shardId);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .build();

        server.start();
        System.out.println("Wallet Server (LEADER for Shard " + shardId + ") started on port " + port);

        server.awaitTermination();
    }
}
