package ds.cw.server;

import ds.cw.common.Account;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WalletStore {

    private final ConcurrentHashMap<String, Account> accounts =
            new ConcurrentHashMap<>();

    private final Set<String> processedRequests =
            ConcurrentHashMap.newKeySet();

    public boolean isDuplicate(String requestId) {
        return !processedRequests.add(requestId);
    }

    public boolean createAccount(String accountId) {
        return accounts.putIfAbsent(accountId, new Account(accountId)) == null;
    }

    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public boolean isProcessed(String requestId) {
        return processedRequests.contains(requestId);
    }

    public void markProcessed(String requestId) {
        processedRequests.add(requestId);
    }
}
