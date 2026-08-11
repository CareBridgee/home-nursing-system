package iti.jets.java.homenursing.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runsImmediatelyWhenNoTransactionIsActive() {
        AtomicInteger ran = new AtomicInteger();
        executor.execute(ran::incrementAndGet);
        assertThat(ran).hasValue(1);
    }

    @Test
    void registersSynchronizationWhenTransactionIsActiveAndRunsOnAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger ran = new AtomicInteger();

        executor.execute(ran::incrementAndGet);

        assertThat(ran).hasValue(0);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.get(0).afterCommit();
        assertThat(ran).hasValue(1);
    }

    @Test
    void runnableExceptionIsSwallowedWithoutTransaction() {
        AtomicInteger ran = new AtomicInteger();
        executor.execute(() -> {
            ran.incrementAndGet();
            throw new IllegalStateException("boom");
        });
        assertThat(ran).hasValue(1);
    }

    @Test
    void runnableExceptionIsSwallowedInsideAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger ran = new AtomicInteger();

        executor.execute(() -> {
            ran.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
        assertThat(ran).hasValue(1);
    }
}
