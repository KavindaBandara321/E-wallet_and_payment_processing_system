package ds.cw.client;

import ds.cw.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class WalletClient {

    private final Map<String, WalletServiceGrpc.WalletServiceBlockingStub> stubs = new HashMap<>();
    private final Map<String, ManagedChannel> channels = new HashMap<>();

    public WalletClient() {
        // Initialize connections to known shards
        // In a real system, these might come from a configuration file or ZooKeeper
        addShard("1", "localhost", 9000);
        addShard("2", "localhost", 9001);
    }

    private void addShard(String shardId, String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        channels.put(shardId, channel);
        stubs.put(shardId, WalletServiceGrpc.newBlockingStub(channel));
    }

    private String getShardForAccount(String accountId) {
        if (accountId.startsWith("1"))
            return "1";
        if (accountId.startsWith("2"))
            return "2";
        return "1";
    }

    private WalletServiceGrpc.WalletServiceBlockingStub getStub(String accountId) {
        String shardId = getShardForAccount(accountId);
        return stubs.get(shardId);
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- SHARDED WALLET CLIENT ---");
            System.out.println("1. Create Account (Shard 1: 1xxx, Shard 2: 2xxx)");
            System.out.println("2. Get Balance");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer Money (Internal or Cross-Shard)");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            try {
                int choice = Integer.parseInt(sc.nextLine());
                switch (choice) {
                    case 1 -> createAccount(sc);
                    case 2 -> getBalance(sc);
                    case 3 -> deposit(sc);
                    case 4 -> withdraw(sc);
                    case 5 -> transfer(sc);
                    case 0 -> {
                        shutdown();
                        return;
                    }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void createAccount(Scanner sc) {
        System.out.print("Enter target Account ID (e.g., 101 or 201): ");
        String id = sc.nextLine();

        CreateAccountRequest req = CreateAccountRequest.newBuilder().setAccountId(id).build();
        try {
            CreateAccountResponse res = getStub(id).createAccount(req);
            System.out.println("RESULT: " + res.getMessage());
        } catch (Exception e) {
            System.out.println("RPC failed: " + e.getMessage());
        }
    }

    private void getBalance(Scanner sc) {
        System.out.print("Account ID: ");
        String id = sc.nextLine();

        BalanceRequest req = BalanceRequest.newBuilder().setAccountId(id).build();
        try {
            BalanceResponse res = getStub(id).getBalance(req);
            if (res.getBalance() < 0) {
                System.out.println("Error: Account not found or wrong shard");
            } else {
                System.out.println("Balance for " + id + ": " + res.getBalance());
            }
        } catch (Exception e) {
            System.out.println("RPC failed: " + e.getMessage());
        }
    }

    private void deposit(Scanner sc) {
        System.out.print("Account ID: ");
        String id = sc.nextLine();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(sc.nextLine());

        TransactionRequest req = TransactionRequest.newBuilder()
                .setAccountId(id)
                .setAmount(amount)
                .setRequestId(UUID.randomUUID().toString())
                .build();
        try {
            TransactionResponse res = getStub(id).deposit(req);
            System.out.println(res.getMessage() + " | New Balance: " + res.getBalance());
        } catch (Exception e) {
            System.out.println("RPC failed: " + e.getMessage());
        }
    }

    private void withdraw(Scanner sc) {
        System.out.print("Account ID: ");
        String id = sc.nextLine();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(sc.nextLine());

        TransactionRequest req = TransactionRequest.newBuilder()
                .setAccountId(id)
                .setAmount(amount)
                .setRequestId(UUID.randomUUID().toString())
                .build();
        try {
            TransactionResponse res = getStub(id).withdraw(req);
            System.out.println(res.getMessage() + " | New Balance: " + res.getBalance());
        } catch (Exception e) {
            System.out.println("RPC failed: " + e.getMessage());
        }
    }

    private void transfer(Scanner sc) {
        System.out.print("From Account ID: ");
        String from = sc.nextLine();
        System.out.print("To Account ID: ");
        String to = sc.nextLine();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(sc.nextLine());

        TransferRequest req = TransferRequest.newBuilder()
                .setFromAccountId(from)
                .setToAccountId(to)
                .setAmount(amount)
                .setRequestId(UUID.randomUUID().toString())
                .build();

        try {
            // Transfers are initiated at the source account's shard
            TransferResponse res = getStub(from).transfer(req);
            System.out.println("TRANSFER RESULT: " + res.getMessage());
            if (res.getSuccess()) {
                System.out.println("Transaction confirmed across shards.");
            }
        } catch (Exception e) {
            System.out.println("Transfer RPC failed: " + e.getMessage());
        }
    }

    private void shutdown() {
        channels.values().forEach(ManagedChannel::shutdown);
        System.out.println("Channels closed. Goodbye!");
    }

    public static void main(String[] args) {
        new WalletClient().start();
    }
}
