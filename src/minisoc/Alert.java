package minisoc;

import java.time.Instant;

/**
 * One finding from a detector. Serialised as a single JSON line so
 * Filebeat / Logstash can pick it up without any extra parsing.
 */
public class Alert {

    private final String type;
    private final String severity;
    private final String sourceIp;
    private final String detail;
    private final Instant time;

    public Alert(String type, String severity, String sourceIp, String detail) {
        this(type, severity, sourceIp, detail, Instant.now());
    }

    public Alert(String type, String severity, String sourceIp, String detail, Instant time) {
        this.type = type;
        this.severity = severity;
        this.sourceIp = sourceIp;
        this.detail = detail;
        this.time = time != null ? time : Instant.now();
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getTime() {
        return time;
    }

    public String toJson() {
        return "{\"timestamp\":\"" + time + "\""
                + ",\"type\":\"" + escape(type) + "\""
                + ",\"severity\":\"" + escape(severity) + "\""
                + ",\"source_ip\":\"" + escape(sourceIp) + "\""
                + ",\"detail\":\"" + escape(detail) + "\"}";
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + type + " from " + sourceIp + " - " + detail;
    }

    /**
     * Parses an Alert from a JSON line previously written by Alert#toJson().
     * Returns null if the line cannot be parsed.
     */
    public static Alert fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            String timestamp = extractField(json, "timestamp");
            String type = extractField(json, "type");
            String severity = extractField(json, "severity");
            String sourceIp = extractField(json, "source_ip");
            String detail = extractField(json, "detail");

            if (type == null || severity == null || sourceIp == null) {
                return null;
            }

            Instant time = Instant.now();
            if (timestamp != null) {
                try {
                    time = Instant.parse(timestamp);
                } catch (Exception ignored) {}
            }

            return new Alert(type, severity, sourceIp, detail != null ? detail : "", time);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractField(String json, String field) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return unescape(m.group(1));
        }
        return null;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 1, i + 5), 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append(next);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // minimal JSON escaping, enough for log snippets
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
