package minisoc;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches sshd lines from auth.log for repeated failed logins.
 * It also flags a successful login from an address that was hammering
 * the server a moment ago, because that is the case that really matters.
 */
public class SshBruteForceDetector implements Detector {

    private final int maxFailures;
    private final long windowMs;

    public SshBruteForceDetector() {
        this(Config.getInt("ssh.max_failures", 5), Config.getLong("ssh.window_ms", 60_000));
    }

    public SshBruteForceDetector(int maxFailures, long windowMs) {
        this.maxFailures = maxFailures;
        this.windowMs = windowMs;
    }

    private static final String IP = "(\\d{1,3}(?:\\.\\d{1,3}){3})";
    private static final Pattern FAILED =
            Pattern.compile("Failed password for (?:invalid user )?(\\S+) from " + IP);
    private static final Pattern ACCEPTED =
            Pattern.compile("Accepted (?:password|publickey) for (\\S+) from " + IP);

    // ip -> timestamps of its recent failed attempts
    private final Map<String, Deque<Long>> failures = new HashMap<>();

    @Override
    public String name() {
        return "ssh-brute-force";
    }

    @Override
    public void reset() {
        failures.clear();
    }

    @Override
    public List<Alert> inspect(String line) {
        Matcher failed = FAILED.matcher(line);
        if (failed.find()) {
            String user = failed.group(1);
            String ip = failed.group(2);
            int count = recordFailure(ip);
            if (count >= maxFailures) {
                return List.of(new Alert("ssh_brute_force", "HIGH", ip,
                        count + " failed logins in the last minute, last user tried: " + user));
            }
            return Collections.emptyList();
        }

        Matcher accepted = ACCEPTED.matcher(line);
        if (accepted.find()) {
            String user = accepted.group(1);
            String ip = accepted.group(2);
            int recent = recentFailures(ip);
            if (recent >= maxFailures) {
                return List.of(new Alert("ssh_login_after_brute_force", "CRITICAL", ip,
                        "login as '" + user + "' succeeded right after " + recent
                                + " failures, account may be compromised"));
            }
        }
        return Collections.emptyList();
    }

    private int recordFailure(String ip) {
        Deque<Long> times = failures.computeIfAbsent(ip, k -> new ArrayDeque<>());
        times.addLast(System.currentTimeMillis());
        return trim(times);
    }

    private int recentFailures(String ip) {
        Deque<Long> times = failures.get(ip);
        return times == null ? 0 : trim(times);
    }

    // drop timestamps that fell out of the window and return what is left
    private int trim(Deque<Long> times) {
        long cutoff = System.currentTimeMillis() - windowMs;
        while (!times.isEmpty() && times.peekFirst() < cutoff) {
            times.removeFirst();
        }
        return times.size();
    }
}
