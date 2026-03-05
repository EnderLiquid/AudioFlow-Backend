package top.enderliquid.audioflow.common.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionHelper implements AutoCloseable {

    private final PlatformTransactionManager txManager;
    private final TransactionStatus status;

    // 构造函数，自动开启事务
    public TransactionHelper(PlatformTransactionManager txManager, TransactionDefinition def) {
        this.txManager = txManager;
        this.status = txManager.getTransaction(def);
    }

    public TransactionHelper(PlatformTransactionManager txManager) {
        this(txManager, new DefaultTransactionDefinition());
    }

    // 手动提交 (try 块结束前必须手动调用！)
    public void commit() {
        txManager.commit(status);
    }

    // 手动回滚 (如有必要)
    public void rollback() {
        txManager.rollback(status);
    }

    // try 块结束时，若未手动提交或回滚，则说明产生异常，自动回滚
    @Override
    public void close() {
        if (!status.isCompleted()) {
            txManager.rollback(status);
        }
    }
}
