package com.portfolio.helpdesk.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {
    private static final String EMAIL = "User@Example.test";
    private static final String IP = "192.0.2.10";

    @Test
    void blocksTheSameAccountAndIpAfterFiveFailuresForFifteenMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; i++) {
            service.recordFailure(EMAIL, IP);
        }

        assertTrue(service.isBlocked("user@example.test", IP));
        assertEquals(900, service.retryAfterSeconds(EMAIL, IP));
    }

    @Test
    void concurrentRequestsCannotReserveMoreThanTheAccountIpLimit() {
        LoginAttemptService service = new LoginAttemptService(Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; i++) {
            assertTrue(service.tryBeginAttempt(EMAIL, IP));
        }
        assertFalse(service.tryBeginAttempt(EMAIL, IP));

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; i++) {
            service.recordFailure(EMAIL, IP);
        }
        assertTrue(service.isBlocked(EMAIL, IP));
    }

    @Test
    void aggregateIpLimitStopsPasswordSprayingAcrossAccountNames() {
        LoginAttemptService service = new LoginAttemptService(Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_IP; i++) {
            service.recordFailure("account-" + i + "@example.test", IP);
        }

        assertTrue(service.isBlocked("another-account@example.test", IP));
        assertEquals(900, service.retryAfterSeconds("another-account@example.test", IP));
    }

    @Test
    void oneAddressCannotLockAnAccountForEveryOtherAddress() {
        LoginAttemptService service = new LoginAttemptService(Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; i++) {
            service.recordFailure(EMAIL, IP);
        }

        assertTrue(service.isBlocked(EMAIL, IP));
        assertFalse(service.isBlocked(EMAIL, "192.0.2.11"));
    }

    @Test
    void expiredFailuresNoLongerBlockLogin() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP; i++) {
            service.recordFailure(EMAIL, IP);
        }

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertFalse(service.isBlocked(EMAIL, IP));
        assertEquals(0, service.retryAfterSeconds(EMAIL, IP));
    }

    @Test
    void successfulLoginClearsPairFailuresButDoesNotResetTheIpAggregate() {
        LoginAttemptService service = new LoginAttemptService(Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC));
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_ACCOUNT_AND_IP - 1; i++) {
            service.recordFailure(EMAIL, IP);
        }

        service.recordSuccess(EMAIL.toLowerCase(), IP);
        assertFalse(service.isBlocked(EMAIL, IP));

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES_PER_IP - 4; i++) {
            service.recordFailure("other-" + i + "@example.test", IP);
        }
        assertTrue(service.isBlocked("another-account@example.test", IP));
    }

    private static final class MutableClock extends Clock {
        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
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
            return currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}
