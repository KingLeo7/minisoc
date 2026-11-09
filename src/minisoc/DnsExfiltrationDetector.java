package minisoc;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches DNS logs (BIND, dnsmasq, or Suricata DNS eve events) for signs of
 * DNS data exfiltration or tunneling (e.g. iodine, dnscat2).
 *
 * It checks for:
 * 1. Long, high-entropy subdomain labels (> 32 chars) typical of base32/base64/hex payloads.
 * 2. Rapid succession of unique subdomains under the same apex domain from one IP.
 */
public class DnsExfiltrationDetector implements Detector {

    private static final int LABEL_LENGTH_THRESHOLD = 32;
    private static final int BURST_QUERY_THRESHOLD = 8;
    private static final long WINDOW_MS = 20_000;

    // Suricata eve.json DNS format: {"event_type":"dns","src_ip":"192.168.56.10",..."rrname":"..."}
    private static final Pattern SURICATA_DNS =
            Pattern.compile("\"event_type\"\\s*:\\s*\"dns\"");
    private static final Pattern SURICATA_SRC =
            Pattern.compile("\"src_ip\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SURICATA_RRNAME =
            Pattern.compile("\"rrname\"\\s*:\\s*\"([^\"]+)\"");

    // BIND / dnsmasq format:
    // query[A] <domain> from <ip>
    // client @... <ip>#... (<domain>): query: <domain>
    private static final Pattern DNSMASQ_QUERY =
            Pattern.compile("query\\[[^\\]]+\\]\\s+(\\S+)\\s+from\\s+(\\d{1,3}(?:\\.\\d{1,3}){3})");
    private static final Pattern BIND_QUERY =
            Pattern.compile("client\\s+(?:@\\S+\\s+)?(\\d{1,3}(?:\\.\\d{1,3}){3})#[0-9]+.*query:\\s+(\\S+)");

    // Key: sourceIp|apexDomain -> List of query timestamps
    private final Map<String, Deque<Long>> burstTracking = new HashMap<>();

    @Override
    public String name() {
        return "dns-exfiltration";
    }

    @Override
    public void reset() {
        burstTracking.clear();
    }

    @Override
    public List<Alert> inspect(String line) {
        String ip = null;
        String query = null;

        if (SURICATA_DNS.matcher(line).find()) {
            Matcher mIp = SURICATA_SRC.matcher(line);
            Matcher mRr = SURICATA_RRNAME.matcher(line);
            if (mIp.find() && mRr.find()) {
                ip = mIp.group(1);
                query = mRr.group(1);
            }
        } else {
            Matcher mDnsmasq = DNSMASQ_QUERY.matcher(line);
            if (mDnsmasq.find()) {
                query = mDnsmasq.group(1);
                ip = mDnsmasq.group(2);
            } else {
                Matcher mBind = BIND_QUERY.matcher(line);
                if (mBind.find()) {
                    ip = mBind.group(1);
                    query = mBind.group(2);
                }
            }
        }

        if (ip == null || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        query = query.toLowerCase();
        if (query.endsWith(".")) {
            query = query.substring(0, query.length() - 1);
        }

        List<Alert> alerts = new ArrayList<>();
        String[] labels = query.split("\\.");

        // 1. Check for abnormally long label (tunneling chunk)
        for (String label : labels) {
            if (label.length() >= LABEL_LENGTH_THRESHOLD) {
                alerts.add(new Alert("dns_exfiltration", "HIGH", ip,
                        "anomalous DNS query label (" + label.length() + " chars): "
                                + shorten(query)));
                return alerts;
            }
        }

        // 2. Check for rapid burst of queries to the same apex domain
        if (labels.length >= 2) {
            String apex = labels[labels.length - 2] + "." + labels[labels.length - 1];
            String trackingKey = ip + "|" + apex;
            long now = System.currentTimeMillis();

            Deque<Long> timestamps = burstTracking.computeIfAbsent(trackingKey, k -> new ArrayDeque<>());
            timestamps.addLast(now);
            long cutoff = now - WINDOW_MS;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.removeFirst();
            }

            if (timestamps.size() >= BURST_QUERY_THRESHOLD) {
                alerts.add(new Alert("dns_tunneling_burst", "HIGH", ip,
                        timestamps.size() + " subdomains queried under " + apex
                                + " in " + (WINDOW_MS / 1000) + "s, potential data tunneling"));
            }
        }

        return alerts;
    }

    private String shorten(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}

