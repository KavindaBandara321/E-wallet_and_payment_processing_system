package ds.cw.server;

import ds.cw.common.Account;
import ds.cw.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;

public class WalletServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletStore store;
    private final String shardId;
    private final Map<String, String> shardAddresses; // ShardID -> host:port

    public WalletServiceImpl(WalletStore store, String shardId) {
        this.store = store;
        this.shardId = shardId;
        this.shardAddresses = new HashMap<>();
        // Hardcoded for assignment scope
        shardAddresses.put("1", "localhost:9000");
        shardAddresses.put("2", "localhost:9001");
    }

    private String getShardForAccount(String accountId) {
        // Simple sharding logic: accounts starting with '1' are in Shard 1, '2' in
        // Shard 2
        if (accountId.startsWith("1"))
            return "1";
        if (accountId.startsWith("2"))
            return "2";
        return "1"; // Default
    }

    private boolean isMyShard(String accountId) {
        return shardId.equals(getShardForAccount(accountId));
    }

    @Override
    public void createAccount(CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver) {

        if (!isMyShard(request.getAccountId())) {
            respondCreateFailure(responseObserver,
                    "Account belongs to Shard " + getShardForAccount(request.getAccountId()));
            return;
        }

        boolean created = store.createAccount(request.getAccountId());
        responseObserver.onNext(CreateAccountResponse.newBuilder()
                .setSuccess(created)
                .setMessage(created ? "Account created in Shard " + shardId : "Account already exists")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getBalance(BalanceRequest request,
            StreamObserver<BalanceResponse> responseObserver) {

        if (!isMyShard(request.getAccountId())) {
            // In a real system, we'd redirect. For now, just error.
            responseObserver.onNext(BalanceResponse.newBuilder().setBalance(-1.0).build());
            responseObserver.onCompleted();
            return;
        }

        Account account = store.getAccount(request.getAccountId());
        double balance = (account == null) ? 0.0 : account.getBalance();

        responseObserver.onNext(BalanceResponse.newBuilder().setBalance(balance).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deposit(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        if (!isMyShard(request.getAccountId())) {
            respondFailure(responseObserver, "Redirect to Shard " + getShardForAccount(request.getAccountId()));
            return;
        }

        Account account = store.getAccount(request.getAccountId());
        if (account == null) {
            respondFailure(responseObserver, "Account not found");
            return;
        }

        if (store.isProcessed(request.getRequestId())) {
            respondSuccess(responseObserver, account.getBalance());
            return;
        }

        account.deposit(request.getAmount());
        store.markProcessed(request.getRequestId());
        respondSuccess(responseObserver, account.getBalance());
    }

    @Override
    public void withdraw(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        if (!isMyShard(request.getAccountId())) {
            respondFailure(responseObserver, "Redirect to Shard " + getShardForAccount(request.getAccountId()));
            return;
        }

        Account account = store.getAccount(request.getAccountId());
        if (account == null) {
            respondFailure(responseObserver, "Account not found");
            return;
        }

        if (store.isProcessed(request.getRequestId())) {
            respondSuccess(responseObserver, account.getBalance());
            return;
        }

        boolean ok = account.withdraw(request.getAmount());
        if (!ok) {
            respondFailure(responseObserver, "Insufficient balance");
            return;
        }

        store.markProcessed(request.getRequestId());
        respondSuccess(responseObserver, account.getBalance());
    }

    @Override
    public void transfer(TransferRequest request, StreamObserver<TransferResponse> responseObserver) {
        String fromId = request.getFromAccountId();
        String toId = request.getToAccountId();
        double amount = request.getAmount();

        if (!isMyShard(fromId)) {
            respondTransferFailure(responseObserver, "Source account must be in Shard " + shardId);
            return;
        }

        Account fromAccount = store.getAccount(fromId);
        if (fromAccount == null) {
            respondTransferFailure(responseObserver, "Source account not found");
            return;
        }

        if (store.isProcessed(request.getRequestId())) {
            respondTransferSuccess(responseObserver, "Already processed");
            return;
        }

        // 1. Withdraw locally
        boolean ok = fromAccount.withdraw(amount);
        if (!ok) {
            respondTransferFailure(responseObserver, "Insufficient balance in Shard " + shardId);
            return;
        }

        // 2. Deposit to destination
        if (isMyShard(toId)) {
            // Intra-shard transfer
            Account toAccount = store.getAccount(toId);
            if (toAccount == null) {
                fromAccount.deposit(amount); // Rollback
                respondTransferFailure(responseObserver, "Destination account not found in Shard " + shardId);
            } else {
                toAccount.deposit(amount);
                store.markProcessed(request.getRequestId());
                respondTransferSuccess(responseObserver, "Intra-shard transfer OK");
            }
        } else {
            // Cross-shard transfer
            String targetShard = getShardForAccount(toId);
            String targetAddr = shardAddresses.get(targetShard);

            try {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddr).usePlaintext().build();
                WalletServiceGrpc.WalletServiceBlockingStub stub = WalletServiceGrpc.newBlockingStub(channel);

                TransactionResponse res = stub.deposit(TransactionRequest.newBuilder()
                        .setAccountId(toId)
                        .setAmount(amount)
                        .setRequestId(request.getRequestId() + "_dep") // Derivative ID for atomicity
                        .build());

                channel.shutdown();

                if (res.getSuccess()) {
                    store.markProcessed(request.getRequestId());
                    respondTransferSuccess(responseObserver, "Cross-shard transfer to Shard " + targetShard + " OK");
                } else {
                    fromAccount.deposit(amount); // Rollback
                    respondTransferFailure(responseObserver, "Target shard rejection: " + res.getMessage());
                }
            } catch (Exception e) {
                fromAccount.deposit(amount); // Rollback
                respondTransferFailure(responseObserver, "Target shard unreachable: " + targetAddr);
            }
        }
    }

    private void respondSuccess(StreamObserver<TransactionResponse> observer, double balance) {
        observer.onNext(TransactionResponse.newBuilder().setSuccess(true).setBalance(balance).setMessage("OK").build());
        observer.onCompleted();
    }

    private void respondFailure(StreamObserver<TransactionResponse> observer, String message) {
        observer.onNext(TransactionResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    private void respondTransferSuccess(StreamObserver<TransferResponse> observer, String msg) {
        observer.onNext(TransferResponse.newBuilder().setSuccess(true).setMessage(msg).build());
        observer.onCompleted();
    }

    private void respondTransferFailure(StreamObserver<TransferResponse> observer, String msg) {
        observer.onNext(TransferResponse.newBuilder().setSuccess(false).setMessage(msg).build());
        observer.onCompleted();
    }

    private void respondCreateFailure(StreamObserver<CreateAccountResponse> observer, String msg) {
        observer.onNext(CreateAccountResponse.newBuilder().setSuccess(false).setMessage(msg).build());
        observer.onCompleted();
    }
}
