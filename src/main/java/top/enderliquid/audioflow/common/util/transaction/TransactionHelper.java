package top.enderliquid.audioflow.common.util.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionHelper {
    @Autowired
    private TransactionTemplate transactionTemplate;

    public void execute(TransactionAction action) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                try {
                    action.run();
                } catch (Exception e) {
                    // 1. 标记回滚
                    status.setRollbackOnly();
                    // 2. 异常处理
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException("发生异常，回滚事务", e);
                }
            }
        });
    }
}
