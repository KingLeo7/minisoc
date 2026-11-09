package minisoc.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Interactive Lab Attack Simulator that injects live attack signatures
 * into the monitored log files so analysts can test MiniSOC detectors in real time.
 */
public class AttackSimulator {

    private static final DateTimeFormatter AUTH_DATE_FMT = DateTimeFormatter.ofPattern("MMM  d HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter HTTP_DATE_FMT = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +0000", Locale.ENGLISH);

    public static void simulateSshBruteForce(String ip, String user) throws IOException {
        Path p = Paths.get("samples/auth.log");
        ensureParentDir(p);
        StringBuilder sb = new StringBuilder();
        String date = LocalDateTime.now().format(AUTH_DATE_FMT);

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            int port = 51000 + (i * 2);
            int pid = 3000 + i;
            sb.append(String.format("%s sensor sshd[%d]: Failed password for %s from %s port %d ssh2\n",
                    date, pid, user, ip, port));
        }
        // 1 accepted attempt
        sb.append(String.format("%s sensor sshd[3005]: Accepted password for %s from %s port 51010 ssh2\n",
                date, user, ip));

        appendToFile(p, sb.toString());
    }

    public static void simulatePortScan(String ip, int numPorts) throws IOException {
        Path p = Paths.get("samples/eve.json");
        ensureParentDir(p);
        StringBuilder sb = new StringBuilder();
        int[] commonPorts = {21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 993, 995, 1433, 1521, 3306, 3389, 5432, 8080, 8443, 9000};
        int count = Math.min(numPorts, commonPorts.length);

        for (int i = 0; i < count; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"event_type\":\"flow\",\"src_ip\":\"%s\",\"src_port\":%d,\"dest_ip\":\"192.168.56.20\",\"dest_port\":%d,\"proto\":\"TCP\"}\n",
                    LocalDateTime.now(), ip, 40000 + i, commonPorts[i]));
        }

        appendToFile(p, sb.toString());
    }

    public static void simulateSqlInjection(String ip) throws IOException {
        Path p = Paths.get("samples/access.log");
        ensureParentDir(p);
        String date = LocalDateTime.now().format(HTTP_DATE_FMT);
        String line = String.format("%s - - [%s] \"GET /dvwa/vulnerabilities/sqli/?id=1%%27+UNION+SELECT+null%%2Cusername%%2Cpassword+FROM+users--+&submit=submit HTTP/1.1\" 200 4520 \"-\" \"Mozilla/5.0\"\n",
                ip, date);
        appendToFile(p, line);
    }

    public static void simulateXssAndTraversal(String ip) throws IOException {
        Path p = Paths.get("samples/access.log");
        ensureParentDir(p);
        String date = LocalDateTime.now().format(HTTP_DATE_FMT);
        String line1 = String.format("%s - - [%s] \"GET /dvwa/vulnerabilities/xss_r/?name=%%3Cscript%%3Ealert(%%27pwned%%27)%%3C%%2Fscript%%3E HTTP/1.1\" 200 3120 \"-\" \"Mozilla/5.0\"\n",
                ip, date);
        String line2 = String.format("%s - - [%s] \"GET /dvwa/../../../../etc/passwd HTTP/1.1\" 404 198 \"-\" \"Mozilla/5.0\"\n",
                ip, date);
        appendToFile(p, line1 + line2);
    }

    public static void simulateScannerTool(String ip, String toolName) throws IOException {
        Path p = Paths.get("samples/access.log");
        ensureParentDir(p);
        String date = LocalDateTime.now().format(HTTP_DATE_FMT);
        String line = String.format("%s - - [%s] \"GET /dvwa/login.php HTTP/1.1\" 200 2410 \"-\" \"%s/1.6\"\n",
                ip, date, toolName);
        appendToFile(p, line);
    }

    public static void simulateArpSpoof(String ip, String legitimateMac, String attackerMac) throws IOException {
        Path p = Paths.get("samples/arp.log");
        ensureParentDir(p);
        String line1 = String.format("%s dev eth1 lladdr %s REACHABLE\n", ip, legitimateMac);
        String line2 = String.format("%s dev eth1 lladdr %s REACHABLE\n", ip, attackerMac);
        appendToFile(p, line1 + line2);
    }

    public static void simulateDnsExfiltration(String ip) throws IOException {
        Path p = Paths.get("samples/dns.log");
        ensureParentDir(p);
        String date = LocalDateTime.now().format(AUTH_DATE_FMT);
        String line = String.format("%s sensor dnsmasq[5012]: query[TXT] dGVzdC1zZWNyZXQtZGF0YS1leGZpbHRyYXRpb24tcGF5bG9hZAo.exfil.attacker.com from %s\n",
                date, ip);
        appendToFile(p, line);
    }

    private static void ensureParentDir(Path p) {
        if (p.getParent() != null && !Files.exists(p.getParent())) {
            try {
                Files.createDirectories(p.getParent());
            } catch (Exception ignored) {}
        }
    }

    private static synchronized void appendToFile(Path p, String content) throws IOException {
        Files.write(p, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}

