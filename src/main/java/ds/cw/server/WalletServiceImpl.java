package ds.cw.server;

import ds.cw.common.Account;
import ds.cw.grpc.*;
import io.grpc.stub.StreamObserver;

public class WalletServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletStore store;

    public WalletServiceImpl(WalletStore store) {
        this.store = store;
    }

    @Override
    public void createAccount(CreateAccountRequest request,
                              StreamObserver<CreateAccountResponse> responseObserver) {

        boolean created = store.createAccount(request.getAccountId());

        CreateAccountResponse response =
                CreateAccountResponse.newBuilder()
                        .setSuccess(created)
                        .setMessage(created ? "Account created"
                                : "Account already exists")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBalance(BalanceRequest request,
                           StreamObserver<BalanceResponse> responseObserver) {

        Account account = store.getAccount(request.getAccountId());

        double balance = (account == null) ? 0.0 : account.getBalance();

        responseObserver.onNext(
                BalanceResponse.newBuilder()
                        .setBalance(balance)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void deposit(TransactionRequest request,
                        StreamObserver<TransactionResponse> responseObserver) {

        if (store.isDuplicate(request.getRequestId())) {
            respondSuccess(responseObserver,
                    store.getAccount(request.getAccountId()).getBalance());
            return;
        }

        Account account = store.getAccount(request.getAccountId());

        if (account == null) {
            respondFailure(responseObserver, "Account not found");
            return;
        }

//        account.deposit(request.getAmount());
        if (store.isProcessed(request.getRequestId())) {
            respondSuccess(responseObserver, account.getBalance());
            return;
        }

        account.deposit(request.getAmount());
        store.markProcessed(request.getRequestId());
        respondSuccess(responseObserver, account.getBalance());

//        respondSuccess(responseObserver, account.getBalance());
    }

    @Override
    public void withdraw(TransactionRequest request,
                         StreamObserver<TransactionResponse> responseObserver) {

        if (store.isDuplicate(request.getRequestId())) {
            respondSuccess(responseObserver,
                    store.getAccount(request.getAccountId()).getBalance());
            return;
        }

        Account account = store.getAccount(request.getAccountId());

        if (account == null) {
            respondFailure(responseObserver, "Account not found");
            return;
        }

        boolean ok = account.withdraw(request.getAmount());

        if (!ok) {
            respondFailure(responseObserver, "Insufficient balance");
            return;
        }
        if (store.isProcessed(request.getRequestId())) {
            respondSuccess(responseObserver, account.getBalance());
            return;
        }

        account.deposit(request.getAmount());
        store.markProcessed(request.getRequestId());
        respondSuccess(responseObserver, account.getBalance());
//        respondSuccess(responseObserver, account.getBalance());
    }

    private void respondSuccess(StreamObserver<TransactionResponse> observer,
                                double balance) {
        observer.onNext(
                TransactionResponse.newBuilder()
                        .setSuccess(true)
                        .setBalance(balance)
                        .setMessage("OK")
                        .build()
        );
        observer.onCompleted();
    }

    private void respondFailure(StreamObserver<TransactionResponse> observer,
                                String message) {
        observer.onNext(
                TransactionResponse.newBuilder()
                        .setSuccess(false)
                        .setBalance(0)
                        .setMessage(message)
                        .build()
        );
        observer.onCompleted();
    }
}
