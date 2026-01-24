package ds.cw.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class WalletServer {

    public static void main(String[] args) throws Exception {

        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 9000;

        WalletStore store = new WalletStore();
        WalletServiceImpl service = new WalletServiceImpl(store);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .build();

        server.start();
        System.out.println("Wallet Server started on port " + port);

        server.awaitTermination();
    }
}
