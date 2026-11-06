package com.example.backend.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tiện ích đăng ký việc cần làm SAU KHI transaction kết thúc (thường là xóa file vật lý),
 * để không xóa file khi DB chưa commit hoặc sắp rollback.
 */
public final class TransactionCallbacks {

    private TransactionCallbacks() {
    }

    /** Chạy sau khi commit thành công. Ngoài transaction thì chạy ngay. */
    public static void afterCommit(Runnable task) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        task.run();
                    }
                }
        );
    }

    /** Chạy chỉ khi transaction bị rollback. Ngoài transaction thì bỏ qua. */
    public static void afterRollback(Runnable task) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            task.run();
                        }
                    }
                }
        );
    }
}
