package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRequestRateLimiterTest {

    @Test
    void limiterShouldIgnoreInvalidInputs() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(fixedClock(100));

        assertThat(limiter.isLimited(null, 1, 60)).isFalse();
        assertThat(limiter.isLimited(" ", 1, 60)).isFalse();
        assertThat(limiter.isLimited("key", 0, 60)).isFalse();
        assertThat(limiter.isLimited("key", 1, 0)).isFalse();

        limiter.recordAttempt(null, 60);
        limiter.recordAttempt(" ", 60);
        limiter.recordAttempt("key", 0);

        assertThat(limiter.isLimited("key", 1, 60)).isFalse();
    }

    @Test
    void limiterShouldLimitAtMaxAttemptsAndClearSpecificKey() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(fixedClock(100));

        limiter.recordAttempt("login:alice", 60);
        limiter.recordAttempt("login:alice", 60);

        assertThat(limiter.isLimited("login:alice", 2, 60)).isTrue();
        assertThat(limiter.isLimited("login:bob", 2, 60)).isFalse();

        limiter.clear("login:alice");

        assertThat(limiter.isLimited("login:alice", 2, 60)).isFalse();
    }

    @Test
    void limiterShouldPruneExpiredAttempts() {
        MutableClock clock = new MutableClock(100);
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(clock);

        limiter.recordAttempt("register:ip", 60);
        assertThat(limiter.isLimited("register:ip", 1, 60)).isTrue();

        clock.setEpochSeconds(161);

        assertThat(limiter.isLimited("register:ip", 1, 60)).isFalse();
    }

    @Test
    void clearShouldIgnoreInvalidKeysAndClearAllShouldRemoveAttempts() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(fixedClock(100));
        limiter.recordAttempt("key", 60);

        limiter.clear(null);
        limiter.clear(" ");
        assertThat(limiter.isLimited("key", 1, 60)).isTrue();

        limiter.clearAll();
        assertThat(limiter.isLimited("key", 1, 60)).isFalse();
    }

    private Clock fixedClock(long epochSeconds) {
        return Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private static final class MutableClock extends Clock {
        private long epochSeconds;

        private MutableClock(long epochSeconds) {
            this.epochSeconds = epochSeconds;
        }

        private void setEpochSeconds(long epochSeconds) {
            this.epochSeconds = epochSeconds;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochSecond(epochSeconds);
        }
    }
}
