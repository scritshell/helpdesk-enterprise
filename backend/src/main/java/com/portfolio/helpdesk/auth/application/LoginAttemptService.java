package com.portfolio.helpdesk.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * In-memory login throttling for the single-instance development deployment.
 *
 * Five failed attempts for the same account/IP pair block that pair for the
 * remaining part of a 15-minute sliding window. A separate, more generous
 * per-IP limit also prevents spraying many different account names from one
 * address. Use a shared store (for example Redis) before running multiple API
 * instances; this limiter is intentionally not a distributed security control.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_FAILURES_PER_ACCOUNT_AND_IP = 5;
    public static final int MAX_FAILURES_PER_IP = 20;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<AccountIpKey, Deque<Instant>> accountIpFailures = new HashMap<>();
    private final Map<String, Deque<Instant>> ipFailures = new HashMap<>();
    private final Map<AccountIpKey, Integer> inFlightAccountIp = new HashMap<>();
    private final Map<String, Integer> inFlightIp = new HashMap<>();

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean isBlocked(String email, String clientIp) {
        Instant now = clock.instant();
        pruneExpired(now);
        AccountIpKey accountIpKey = key(email, clientIp);
        String ip = normalizeIp(clientIp);
        Deque<Instant> pairAttempts = accountIpFailures.get(accountIpKey);
        Deque<Instant> addressAttempts = ipFailures.get(ip);
        return size(pairAttempts) + inFlightAccountIp.getOrDefault(accountIpKey, 0) >= MAX_FAILURES_PER_ACCOUNT_AND_IP
            || size(addressAttempts) + inFlightIp.getOrDefault(ip, 0) >= MAX_FAILURES_PER_IP;
    }

    /** Atomically reserves a login attempt so parallel requests cannot race past the limit. */
    public synchronized boolean tryBeginAttempt(String email, String clientIp) {
        Instant now = clock.instant();
        pruneExpired(now);
        AccountIpKey accountIpKey = key(email, clientIp);
        String ip = normalizeIp(clientIp);
        Deque<Instant> pairAttempts = accountIpFailures.get(accountIpKey);
        Deque<Instant> addressAttempts = ipFailures.get(ip);
        if (size(pairAttempts) + inFlightAccountIp.getOrDefault(accountIpKey, 0) >= MAX_FAILURES_PER_ACCOUNT_AND_IP
            || size(addressAttempts) + inFlightIp.getOrDefault(ip, 0) >= MAX_FAILURES_PER_IP) {
            return false;
        }
        inFlightAccountIp.merge(accountIpKey, 1, Integer::sum);
        inFlightIp.merge(ip, 1, Integer::sum);
        return true;
    }

    /** Seconds until all currently active limits for this request expire. */
    public synchronized long retryAfterSeconds(String email, String clientIp) {
        Instant now = clock.instant();
        pruneExpired(now);
        AccountIpKey accountIpKey = key(email, clientIp);
        String ip = normalizeIp(clientIp);
        Deque<Instant> pairAttempts = accountIpFailures.get(accountIpKey);
        Deque<Instant> addressAttempts = ipFailures.get(ip);

        boolean pairAtLimit = size(pairAttempts) + inFlightAccountIp.getOrDefault(accountIpKey, 0)
            >= MAX_FAILURES_PER_ACCOUNT_AND_IP;
        boolean ipAtLimit = size(addressAttempts) + inFlightIp.getOrDefault(ip, 0) >= MAX_FAILURES_PER_IP;
        long pairRetry = pairAtLimit
            ? (size(pairAttempts) >= MAX_FAILURES_PER_ACCOUNT_AND_IP ? secondsUntilWindowExpires(pairAttempts, now) : 1) : 0;
        long ipRetry = ipAtLimit
            ? (size(addressAttempts) >= MAX_FAILURES_PER_IP ? secondsUntilWindowExpires(addressAttempts, now) : 1) : 0;
        return Math.max(pairRetry, ipRetry);
    }

    public synchronized void recordFailure(String email, String clientIp) {
        Instant now = clock.instant();
        pruneExpired(now);
        releaseReservation(email, clientIp);
        accountIpFailures.computeIfAbsent(key(email, clientIp), ignored -> new ArrayDeque<>()).addLast(now);
        ipFailures.computeIfAbsent(normalizeIp(clientIp), ignored -> new ArrayDeque<>()).addLast(now);
    }

    /**
     * A successful login clears the account/IP pair's failed attempts. The IP
     * aggregate is deliberately retained so a successful login to one account
     * cannot reset an ongoing password-spraying counter for that address.
     */
    public synchronized void recordSuccess(String email, String clientIp) {
        releaseReservation(email, clientIp);
        accountIpFailures.remove(key(email, clientIp));
    }

    /** Releases a reservation if authentication failed for an unexpected reason. */
    public synchronized void cancelAttempt(String email, String clientIp) {
        releaseReservation(email, clientIp);
    }

    private void releaseReservation(String email, String clientIp) {
        AccountIpKey accountIpKey = key(email, clientIp);
        String ip = normalizeIp(clientIp);
        decrement(inFlightAccountIp, accountIpKey);
        decrement(inFlightIp, ip);
    }

    private static <K> void decrement(Map<K, Integer> inFlight, K key) {
        Integer count = inFlight.get(key);
        if (count == null) return;
        if (count <= 1) inFlight.remove(key);
        else inFlight.put(key, count - 1);
    }

    private void pruneExpired(Instant now) {
        pruneMap(accountIpFailures, now);
        pruneMap(ipFailures, now);
    }

    private static <K> void pruneMap(Map<K, Deque<Instant>> attempts, Instant now) {
        Iterator<Map.Entry<K, Deque<Instant>>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Instant> timestamps = iterator.next().getValue();
            while (!timestamps.isEmpty() && !timestamps.peekFirst().plus(WINDOW).isAfter(now)) {
                timestamps.removeFirst();
            }
            if (timestamps.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static int size(Deque<Instant> attempts) {
        return attempts == null ? 0 : attempts.size();
    }

    private static long secondsUntilWindowExpires(Deque<Instant> attempts, Instant now) {
        if (attempts == null || attempts.isEmpty()) {
            return 0;
        }
        Duration remaining = Duration.between(now, attempts.peekFirst().plus(WINDOW));
        if (remaining.isNegative() || remaining.isZero()) {
            return 0;
        }
        long seconds = remaining.getSeconds();
        return remaining.getNano() == 0 ? seconds : seconds + 1;
    }

    private static AccountIpKey key(String email, String clientIp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return new AccountIpKey(normalizedEmail, normalizeIp(clientIp));
    }

    private static String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private record AccountIpKey(String email, String ip) {}
}
