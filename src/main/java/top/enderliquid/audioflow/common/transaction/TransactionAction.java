package top.enderliquid.audioflow.common.transaction;

@FunctionalInterface
public interface TransactionAction {
    void run() throws Exception;
}