package id.ac.ui.cs.advprog.yomu.auth.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRequestRateLimiter {

    private final Map<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRequestRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isLimited(String key, int maxAttempts, long windowSeconds) {
        if (key == null || key.isBlank() || maxAttempts <= 0 || windowSeconds <= 0) {
            return false;
        }
        Deque<Long> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            pruneExpired(attempts, windowSeconds);
            return attempts.size() >= maxAttempts;
        }
    }

    public void recordAttempt(String key, long windowSeconds) {
        if (key == null || key.isBlank() || windowSeconds <= 0) {
            return;
        }
        Deque<Long> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            pruneExpired(attempts, windowSeconds);
            attempts.addLast(nowEpochSeconds());
        }
    }

    public void clear(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        attemptsByKey.remove(key);
    }

    public void clearAll() {
        attemptsByKey.clear();
    }

    private void pruneExpired(Deque<Long> attempts, long windowSeconds) {
        long cutoff = nowEpochSeconds() - windowSeconds;
        while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
            attempts.removeFirst();
        }
    }

    private long nowEpochSeconds() {
        return clock.instant().getEpochSecond();
    }
}
