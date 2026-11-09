package minisoc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Final stop for alerts: prints them and appends them to a JSON-lines
 * file that Filebeat (or anything else) can ship to the dashboard.
 *
 * A scan can produce hundreds of matching lines, so the same alert type
 * for the same source address is only reported once per cooldown period.
 */
public class AlertSink {

    private final Path outFile;
    private final long cooldownMs;
    private final Map<String, Long> lastEmitted = new HashMap<>();
    private final Map<String, Integer> counts = new TreeMap<>();
    private final List<Consumer<Alert>> listeners = new CopyOnWriteArrayList<>();

    public AlertSink(Path outFile, long cooldownMs) {
        this.outFile = outFile;
        this.cooldownMs = cooldownMs;
    }

    public void addListener(Consumer<Alert> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<Alert> listener) {
        listeners.remove(listener);
    }

    public synchronized void clear() {
        lastEmitted.clear();
        counts.clear();
    }

    public synchronized Map<String, Integer> getCounts() {
        return new TreeMap<>(counts);
    }

    public synchronized void emit(Alert alert) {
        String key = alert.getType() + "|" + alert.getSourceIp();
        long now = System.currentTimeMillis();

        Long last = lastEmitted.get(key);
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastEmitted.put(key, now);
        counts.merge(alert.getType(), 1, Integer::sum);

        System.out.println(alert);
        if (outFile != null) {
            try {
                Files.write(outFile, (alert.toJson() + "\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("could not write to " + outFile + ": " + e.getMessage());
            }
        }

        for (Consumer<Alert> listener : listeners) {
            try {
                listener.accept(alert);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void printSummary() {
        System.out.println();
        if (counts.isEmpty()) {
            System.out.println("No alerts raised.");
            return;
        }
        System.out.println("Alert summary:");
        counts.forEach((type, n) -> System.out.println("  " + type + ": " + n));
    }
}
