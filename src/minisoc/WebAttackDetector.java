package minisoc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks at web server access logs (Apache / nginx combined format) for
 * SQL injection, XSS and path traversal payloads in the request line,
 * and for the user agents of well known attack tools.
 */
public class WebAttackDetector implements Detector {

    // 192.168.56.10 - - [08/Nov/2026:17:30:05 +0530] "GET /path?x=1 HTTP/1.1" 200 ...
    private static final Pattern ACCESS_LINE =
            Pattern.compile("^(\\S+) \\S+ \\S+ \\[[^\\]]*\\] \"[A-Z]+ (\\S+)[^\"]*\"");

    private static final String[] SCANNER_AGENTS = {
            "sqlmap", "nikto", "nmap scripting engine", "dirbuster", "gobuster"
    };

    private static class Rule {
        final String type;
        final String severity;
        final Pattern pattern;

        Rule(String type, String severity, String regex) {
            this.type = type;
            this.severity = severity;
            this.pattern = Pattern.compile(regex);
        }
    }

    private final List<Rule> rules = new ArrayList<>();

    public WebAttackDetector() {
        // the request is lower-cased and URL-decoded before matching
        rules.add(new Rule("sql_injection", "HIGH",
                "union\\s+select|\\bor\\s+1\\s*=\\s*1|'\\s*or\\s*'[^']*'\\s*=\\s*'"
                        + "|sleep\\s*\\(\\s*\\d+|benchmark\\s*\\(|information_schema|;\\s*drop\\s+table"));
        rules.add(new Rule("xss_attempt", "MEDIUM",
                "<script|javascript:|onerror\\s*=|onload\\s*="));
        rules.add(new Rule("path_traversal", "MEDIUM",
                "\\.\\./\\.\\./|/etc/passwd|\\.\\.\\\\"));
    }

    @Override
    public String name() {
        return "web-attack";
    }

    @Override
    public List<Alert> inspect(String line) {
        Matcher m = ACCESS_LINE.matcher(line);
        if (!m.find()) {
            return Collections.emptyList();
        }

        String ip = m.group(1);
        String target = decode(m.group(2));
        List<Alert> alerts = new ArrayList<>();

        for (Rule rule : rules) {
            if (rule.pattern.matcher(target).find()) {
                alerts.add(new Alert(rule.type, rule.severity, ip, "request: " + shorten(target)));
            }
        }

        String lowerLine = line.toLowerCase();
        for (String agent : SCANNER_AGENTS) {
            if (lowerLine.contains(agent)) {
                alerts.add(new Alert("scanner_tool_detected", "MEDIUM", ip,
                        "user agent looks like " + agent));
                break;
            }
        }
        return alerts;
    }

    // decode twice, attackers like to double-encode
    private String decode(String raw) {
        String s = raw;
        for (int i = 0; i < 2; i++) {
            try {
                s = URLDecoder.decode(s, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                break; // broken % sequence, keep what we have
            }
        }
        return s.toLowerCase();
    }

    private String shorten(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "..." : s;
    }
}
