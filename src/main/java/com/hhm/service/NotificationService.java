package com.hhm.service;

import com.hhm.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationService {

    // CopyOnWriteArrayList: written once per notification (infrequent), iterated by any
    // thread reading the audit log (frequent). Safe for concurrent iteration without
    // synchronization — each write produces a fresh copy of the internal array.
    private final CopyOnWriteArrayList<String> recentRecipients = new CopyOnWriteArrayList<>();

    // Entry point called by Spring after the DB transaction commits.
    // @Async("dbLookupExecutor") runs this on the configured pool thread —
    // the HTTP request has already returned before this executes.
    // @TransactionalEventListener(AFTER_COMMIT) ensures we only send if the save succeeded.
    @Async("dbLookupExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public CompletableFuture<Void> onUserRegistered(UserRegisteredEvent event) {
        String type = event.type() != null ? event.type().name() : "User";
        return sendNotification("Welcome " + type, event.email());
    }

    // Returns CompletableFuture<Void> so callers can chain or observe completion.
    // The 2-second sleep simulates SMTP latency; in production replace with
    // JavaMailSender or an HTTP call to an email API.
    private CompletableFuture<Void> sendNotification(String type, String email) {
        log.info("Sending {} notification to {}", type, email);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Notification interrupted for {}", email);
            return CompletableFuture.completedFuture(null);
        }
        recentRecipients.add(email);
        log.info("{} notification sent to {}", type, email);
        return CompletableFuture.completedFuture(null);
    }

    // Safe to call from any thread — CopyOnWriteArrayList iterator reflects
    // the snapshot at the time of the call, never throws ConcurrentModificationException.
    public List<String> getRecentRecipients() {
        return List.copyOf(recentRecipients);
    }
}

