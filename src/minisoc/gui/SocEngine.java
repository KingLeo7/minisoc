package minisoc.gui;

import minisoc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller engine that connects MiniSOC detectors, LogTailers, and AlertSink
 * with the GUI interface.
 */
public class SocEngine {

    public enum State {
        IDLE, MONITORING, REPLAYING, STOPPED
    }

    public interface StateListener {
        void onStateChanged(State state, String message);
    }

    public interface LineListener {
        void onLine(String sourceName, String line);
    }

    public interface AlertListener {
        void onAlert(Alert alert);
    }

    private final List<Detector> detectors = new ArrayList<>();
    private final Map<String, Boolean> detectorEnabled = new HashMap<>();

    private List<Path> logPaths = new ArrayList<>();
    private String outFile = "alerts.json";
    private long cooldownMs = 30_000;

    private AlertSink sink;
    private final List<LogTailer> activeTailers = new ArrayList<>();
    private final List<Thread> activeThreads = new ArrayList<>();

    private volatile State currentState = State.IDLE;
    private final AtomicLong totalLinesParsed = new AtomicLong(0);

    private final List<StateListener> stateListeners = new CopyOnWriteArrayList<>();
    private final List<LineListener> lineListeners = new CopyOnWriteArrayList<>();
    private final List<AlertListener> alertListeners = new CopyOnWriteArrayList<>();

    public SocEngine() {
        // Register default detectors
        registerDetector(new SshBruteForceDetector());
        registerDetector(new PortScanDetector());
        registerDetector(new WebAttackDetector());
        registerDetector(new ArpSpoofDetector());
        registerDetector(new DnsExfiltrationDetector());

        // Default to sample logs if they exist
        setDefaultLogPaths();

        initSink();
    }

    private void registerDetector(Detector d) {
        detectors.add(d);
        detectorEnabled.put(d.name(), true);
    }

    public void setDefaultLogPaths() {
        logPaths.clear();
        logPaths.add(Paths.get("samples/auth.log"));
        logPaths.add(Paths.get("samples/access.log"));
        logPaths.add(Paths.get("samples/eve.json"));
        logPaths.add(Paths.get("samples/arp.log"));
        logPaths.add(Paths.get("samples/dns.log"));
    }

    private void initSink() {
        Path out = outFile != null && !outFile.isBlank() ? Paths.get(outFile) : null;
        sink = new AlertSink(out, cooldownMs);
        sink.addListener(alert -> {
            for (AlertListener l : alertListeners) {
                try {
                    l.onAlert(alert);
                } catch (Exception ignored) {}
            }
        });
    }

    public void addStateListener(StateListener l) {
        stateListeners.add(l);
    }

    public void addLineListener(LineListener l) {
        lineListeners.add(l);
    }

    public void addAlertListener(AlertListener l) {
        alertListeners.add(l);
    }

    public State getState() {
        return currentState;
    }

    private void setState(State state, String message) {
        this.currentState = state;
        for (StateListener l : stateListeners) {
            try {
                l.onStateChanged(state, message);
            } catch (Exception ignored) {}
        }
    }

    public boolean isDetectorEnabled(String name) {
        return detectorEnabled.getOrDefault(name, true);
    }

    public void setDetectorEnabled(String name, boolean enabled) {
        detectorEnabled.put(name, enabled);
    }

    public List<Path> getLogPaths() {
        return new ArrayList<>(logPaths);
    }

    public void setLogPaths(List<Path> paths) {
        this.logPaths = new ArrayList<>(paths);
    }

    public String getOutFile() {
        return outFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
        initSink();
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long ms) {
        this.cooldownMs = ms;
        initSink();
    }

    public long getTotalLinesParsed() {
        return totalLinesParsed.get();
    }

    /**
     * Starts live tailing mode on the configured log files.
     */
    public synchronized void startLive() {
        if (currentState == State.MONITORING || currentState == State.REPLAYING) {
            stop();
        }

        resetDetectors();
        initSink();
        activeTailers.clear();
        activeThreads.clear();

        List<Path> existing = filterExisting(logPaths);
        if (existing.isEmpty()) {
            setState(State.STOPPED, "No valid log files to monitor");
            return;
        }

        setState(State.MONITORING, "Monitoring " + existing.size() + " log files live...");

        for (Path path : existing) {
            String sourceName = path.getFileName().toString();
            LogTailer tailer = new LogTailer(path, false, line -> handleLine(sourceName, line));
            Thread t = new Thread(tailer, "tail-" + sourceName);
            activeTailers.add(tailer);
            activeThreads.add(t);
            t.start();
        }
    }

    /**
     * Starts replay mode on the configured log files.
     */
    public synchronized void startReplay(Runnable onComplete) {
        if (currentState == State.MONITORING || currentState == State.REPLAYING) {
            stop();
        }

        resetDetectors();
        initSink();
        activeTailers.clear();
        activeThreads.clear();

        List<Path> existing = filterExisting(logPaths);
        if (existing.isEmpty()) {
            setState(State.STOPPED, "No valid log files to replay");
            return;
        }

        setState(State.REPLAYING, "Replaying " + existing.size() + " log files...");

        Thread replayMaster = new Thread(() -> {
            List<Thread> replayThreads = new ArrayList<>();
            for (Path path : existing) {
                String sourceName = path.getFileName().toString();
                LogTailer tailer = new LogTailer(path, true, line -> handleLine(sourceName, line));
                Thread t = new Thread(tailer, "replay-" + sourceName);
                synchronized (this) {
                    activeTailers.add(tailer);
                    activeThreads.add(t);
                }
                replayThreads.add(t);
                t.start();
            }

            for (Thread t : replayThreads) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {}
            }

            setState(State.IDLE, "Replay completed successfully");
            if (onComplete != null) {
                onComplete.run();
            }
        }, "replay-master");

        replayMaster.start();
    }

    /**
     * Stops all active tailer threads.
     */
    public synchronized void stop() {
        for (LogTailer tailer : activeTailers) {
            tailer.stop();
        }
        for (Thread t : activeThreads) {
            t.interrupt();
        }
        activeTailers.clear();
        activeThreads.clear();
        setState(State.STOPPED, "Sensor stopped");
    }

    public synchronized void clearAlerts() {
        sink.clear();
        resetDetectors();
    }

    public synchronized void resetDetectors() {
        for (Detector d : detectors) {
            d.reset();
        }
    }

    private synchronized void handleLine(String sourceName, String line) {
        totalLinesParsed.incrementAndGet();

        // Dispatch line to live viewer
        for (LineListener l : lineListeners) {
            try {
                l.onLine(sourceName, line);
            } catch (Exception ignored) {}
        }

        // Offer line to active detectors
        for (Detector detector : detectors) {
            if (!detectorEnabled.getOrDefault(detector.name(), true)) {
                continue;
            }
            try {
                for (Alert alert : detector.inspect(line)) {
                    sink.emit(alert);
                }
            } catch (Exception ignored) {}
        }
    }

    private List<Path> filterExisting(List<Path> paths) {
        List<Path> res = new ArrayList<>();
        for (Path p : paths) {
            if (Files.exists(p)) {
                res.add(p);
            }
        }
        return res;
    }

    /**
     * Reads previous alerts from alerts.json.
     */
    public List<Alert> loadAlertsFromFile(Path path) {
        List<Alert> loaded = new ArrayList<>();
        if (!Files.exists(path)) return loaded;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Alert a = Alert.fromJson(line);
                if (a != null) {
                    loaded.add(a);
                }
            }
        } catch (IOException ignored) {}
        return loaded;
    }

    public void exportJson(Path file, List<Alert> alerts) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Alert a : alerts) {
            sb.append(a.toJson()).append("\n");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void exportCsv(Path file, List<Alert> alerts) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,Severity,Type,Source_IP,Detail\n");
        for (Alert a : alerts) {
            sb.append("\"").append(a.getTime()).append("\",")
              .append("\"").append(escapeCsv(a.getSeverity())).append("\",")
              .append("\"").append(escapeCsv(a.getType())).append("\",")
              .append("\"").append(escapeCsv(a.getSourceIp())).append("\",")
              .append("\"").append(escapeCsv(a.getDetail())).append("\"\n");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String escapeCsv(String s) {
        return s != null ? s.replace("\"", "\"\"") : "";
    }
}

