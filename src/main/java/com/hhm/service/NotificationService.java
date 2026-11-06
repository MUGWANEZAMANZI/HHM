package com.hhm.service;

import com.hhm.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class NotificationService {

    // Entry point called by Spring after the DB transaction commits.
    // @Async runs this on a worker thread — the HTTP request has already returned.
    // @TransactionalEventListener(AFTER_COMMIT) ensures we only send if the save succeeded.
    @Async
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
        log.info("{} notification sent to {}", type, email);
        return CompletableFuture.completedFuture(null);
    }
}

