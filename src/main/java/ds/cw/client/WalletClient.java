package ds.cw.client;

import ds.cw.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;
import java.util.UUID;

public class WalletClient {

    private final WalletServiceGrpc.WalletServiceBlockingStub stub;

    public WalletClient(String host, int port) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();

        stub = WalletServiceGrpc.newBlockingStub(channel);
    }

    public void start() {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- WALLET CLIENT ---");
            System.out.println("1. Create Account");
            System.out.println("2. Get Balance");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1 -> createAccount(sc);
                case 2 -> getBalance(sc);
                case 3 -> deposit(sc);
                case 4 -> withdraw(sc);
                case 0 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

//    private void createAccount(Scanner sc) {
//        System.out.print("Account ID: ");
//        String id = sc.nextLine();
//
//        CreateAccountResponse res =
//                stub.createAccount(
//                        CreateAccountRequest.newBuilder()
//                                .setAccountId(id)
//                                .build()
//                );
//
//        System.out.println(res.getMessage());
//    }
private void createAccount(Scanner sc) {
    System.out.print("Account ID: ");
    String id = sc.nextLine();

    CreateAccountRequest request =
            CreateAccountRequest.newBuilder()
                    .setAccountId(id)
                    .build();

    try {
        CreateAccountResponse res = stub.createAccount(request);
        System.out.println(res.getMessage());

    } catch (Exception e) {
        System.out.println("Server error, retrying...");
        try {
            CreateAccountResponse res = stub.createAccount(request);
            System.out.println(res.getMessage());
        } catch (Exception ex) {
            System.out.println("Failed to create account");
        }
    }
}

    private void getBalance(Scanner sc) {
        System.out.print("Account ID: ");
        String id = sc.nextLine();

        BalanceRequest request =
                BalanceRequest.newBuilder()
                        .setAccountId(id)
                        .build();

        try {
            BalanceResponse res = stub.getBalance(request);
            System.out.println("Balance: " + res.getBalance());

        } catch (Exception e) {
            System.out.println("Server unavailable, retrying...");
            try {
                BalanceResponse res = stub.getBalance(request);
                System.out.println("Balance: " + res.getBalance());
            } catch (Exception ex) {
                System.out.println("Failed to get balance");
            }
        }
    }


//    private void getBalance(Scanner sc) {
//        System.out.print("Account ID: ");
//        String id = sc.nextLine();
//
//        BalanceResponse res =
//                stub.getBalance(
//                        BalanceRequest.newBuilder()
//                                .setAccountId(id)
//                                .build()
//                );
//
//        System.out.println("Balance: " + res.getBalance());
//    }

//    private void deposit(Scanner sc) {
//        System.out.print("Account ID: ");
//        String id = sc.nextLine();
//
//        System.out.print("Amount: ");
//        double amount = sc.nextDouble();
//        sc.nextLine();
//
//        TransactionResponse res =
//                stub.deposit(
//                        TransactionRequest.newBuilder()
//                                .setAccountId(id)
//                                .setAmount(amount)
//                                .build()
//                );
//
//        System.out.println(res.getMessage()
//                + " | Balance: " + res.getBalance());
//    }

    private void deposit(Scanner sc) {
        System.out.print("Account ID: ");
        String id = sc.nextLine();

        System.out.print("Amount: ");
        double amount = sc.nextDouble();
        sc.nextLine();

//        TransactionRequest request =
//                TransactionRequest.newBuilder()
//                        .setAccountId(id)
//                        .setAmount(amount)
//                        .build();
        TransactionRequest request =
                TransactionRequest.newBuilder()
                        .setAccountId(id)
                        .setAmount(amount)
                        .setRequestId(UUID.randomUUID().toString())
                        .build();

        try {
            TransactionResponse res = stub.deposit(request);
            System.out.println(res.getMessage()
                    + " | Balance: " + res.getBalance());

        } catch (Exception e) {
            System.out.println("Deposit failed, retrying...");

            try {
                TransactionResponse res = stub.deposit(request);
                System.out.println(res.getMessage()
                        + " | Balance: " + res.getBalance());
            } catch (Exception ex) {
                System.out.println("Deposit failed after retry");
            }
        }
    }


//    private void withdraw(Scanner sc) {
//        System.out.print("Account ID: ");
//        String id = sc.nextLine();
//
//        System.out.print("Amount: ");
//        double amount = sc.nextDouble();
//        sc.nextLine();
//
//        TransactionResponse res =
//                stub.withdraw(
//                        TransactionRequest.newBuilder()
//                                .setAccountId(id)
//                                .setAmount(amount)
//                                .build()
//                );
//
//        System.out.println(res.getMessage()
//                + " | Balance: " + res.getBalance());
//    }
private void withdraw(Scanner sc) {
    System.out.print("Account ID: ");
    String id = sc.nextLine();

    System.out.print("Amount: ");
    double amount = sc.nextDouble();
    sc.nextLine();

//    TransactionRequest request =
//            TransactionRequest.newBuilder()
//                    .setAccountId(id)
//                    .setAmount(amount)
//                    .build();

    TransactionRequest request =
            TransactionRequest.newBuilder()
                    .setAccountId(id)
                    .setAmount(amount)
                    .setRequestId(UUID.randomUUID().toString())
                    .build();
    try {
        TransactionResponse res = stub.withdraw(request);
        System.out.println(res.getMessage()
                + " | Balance: " + res.getBalance());

    } catch (Exception e) {
        System.out.println("Withdraw failed, retrying...");

        try {
            TransactionResponse res = stub.withdraw(request);
            System.out.println(res.getMessage()
                    + " | Balance: " + res.getBalance());
        } catch (Exception ex) {
            System.out.println("Withdraw failed after retry");
        }
    }
}


    public static void main(String[] args) {

        String host = "localhost";
        int port = 9000;

        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        WalletClient client = new WalletClient(host, port);
        client.start();
    }
}
