package minisoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Cowrie SSH/Telnet honeypot logs (JSON format) to detect attacker
 * entrapment, shell commands, malware payloads, and proxy pivot attempts.
 */
public class CowrieHoneypotDetector implements Detector {

    private static final Pattern EVENT_ID = Pattern.compile("\"eventid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SRC_IP = Pattern.compile("\"src_ip\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern USERNAME = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern INPUT_CMD = Pattern.compile("\"input\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DST_IP = Pattern.compile("\"dst_ip\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DST_PORT = Pattern.compile("\"dst_port\"\\s*:\\s*(\\d+)");

    private static final Pattern SUSPICIOUS_CMDS = Pattern.compile(
            "\\b(wget|curl|chmod\\s+\\+x|/etc/shadow|/etc/passwd|python\\s+-c|nc\\s+-e|"
                    + "bash\\s+-i|iptables\\s+-F|systemctl\\s+stop|useradd|crontab|insmod|rm\\s+-rf)\\b|"
                    + "\\|\\s*(sh|bash)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String name() {
        return "cowrie-honeypot";
    }

    @Override
    public List<Alert> inspect(String line) {
        if (!line.contains("\"cowrie.")) {
            return Collections.emptyList();
        }

        Matcher mEvent = EVENT_ID.matcher(line);
        Matcher mSrc = SRC_IP.matcher(line);
        if (!mEvent.find() || !mSrc.find()) {
            return Collections.emptyList();
        }

        String eventId = mEvent.group(1);
        String ip = mSrc.group(1);
        List<Alert> alerts = new ArrayList<>();

        switch (eventId) {
            case "cowrie.login.success":
                Matcher mUser = USERNAME.matcher(line);
                String user = mUser.find() ? mUser.group(1) : "unknown";
                alerts.add(new Alert("honeypot_login", "HIGH", ip,
                        "attacker trapped in honeypot, logged in as '" + user + "'"));
                break;

            case "cowrie.command.input":
                Matcher mCmd = INPUT_CMD.matcher(line);
                if (mCmd.find()) {
                    String cmd = mCmd.group(1);
                    if (SUSPICIOUS_CMDS.matcher(cmd).find()) {
                        alerts.add(new Alert("honeypot_suspicious_command", "HIGH", ip,
                                "honeypot shell command: " + shorten(cmd)));
                    }
                }
                break;

            case "cowrie.session.file_download":
                Matcher mUrl = URL.matcher(line);
                String url = mUrl.find() ? mUrl.group(1) : "unknown payload";
                alerts.add(new Alert("honeypot_malware_download", "CRITICAL", ip,
                        "malware dropper downloaded into honeypot from " + shorten(url)));
                break;

            case "cowrie.direct-tcpip.request":
                Matcher mDstIp = DST_IP.matcher(line);
                Matcher mDstPort = DST_PORT.matcher(line);
                if (mDstIp.find() && mDstPort.find()) {
                    alerts.add(new Alert("honeypot_proxy_abuse", "HIGH", ip,
                            "honeypot proxy/tunneling request to " + mDstIp.group(1) + ":" + mDstPort.group(1)));
                }
                break;
        }

        return alerts;
    }

    private String shorten(String s) {
        return s.length() > 70 ? s.substring(0, 70) + "..." : s;
    }
}

