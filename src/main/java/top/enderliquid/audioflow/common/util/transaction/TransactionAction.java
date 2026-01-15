package top.enderliquid.audioflow.common.util.transaction;

@FunctionalInterface
public interface TransactionAction {
    void run() throws Exception;
}