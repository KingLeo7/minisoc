package minisoc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Suricata's eve.json and counts how many different destination
 * ports each source address touched in a short window. A normal client
 * talks to a handful of ports, a scanner touches dozens.
 *
 * The JSON is picked apart with regexes on purpose, so the project has
 * no external dependencies and compiles with plain javac.
 */
public class PortScanDetector implements Detector {

    private static final int PORT_THRESHOLD = 15;
    private static final long WINDOW_MS = 10_000;

    private static final Pattern FLOW_EVENT = Pattern.compile("\"event_type\"\\s*:\\s*\"flow\"");
    private static final Pattern SRC_IP = Pattern.compile("\"src_ip\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEST_PORT = Pattern.compile("\"dest_port\"\\s*:\\s*(\\d{1,5})");

    // source ip -> (destination port -> last time we saw it)
    private final Map<String, Map<Integer, Long>> seen = new HashMap<>();

    @Override
    public String name() {
        return "port-scan";
    }

    @Override
    public void reset() {
        seen.clear();
    }

    @Override
    public List<Alert> inspect(String line) {
        if (!FLOW_EVENT.matcher(line).find()) {
            return Collections.emptyList();
        }

        Matcher src = SRC_IP.matcher(line);
        Matcher port = DEST_PORT.matcher(line);
        if (!src.find() || !port.find()) {
            return Collections.emptyList();
        }

        String ip = src.group(1);
        int destPort = Integer.parseInt(port.group(1));
        long now = System.currentTimeMillis();

        Map<Integer, Long> ports = seen.computeIfAbsent(ip, k -> new HashMap<>());
        ports.put(destPort, now);
        ports.values().removeIf(t -> now - t > WINDOW_MS);

        if (ports.size() >= PORT_THRESHOLD) {
            return List.of(new Alert("port_scan", "HIGH", ip,
                    "touched " + ports.size() + " different ports in "
                            + (WINDOW_MS / 1000) + " seconds"));
        }
        return Collections.emptyList();
    }
}
