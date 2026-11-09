# MiniSOC

A small log-based intrusion detector written in plain Java (no external libraries). It tails log files from a lab environment, applies a few detection rules, and writes alerts as JSON lines that can be shipped to a dashboard such as ELK or Wazuh.

Built for an isolated VirtualBox lab: Kali attacker, a vulnerable target (DVWA / Juice Shop), a sensor VM running this tool plus Suricata, and a dashboard VM.

## What it detects

| Detector | Log source | Rule (defaults) |
|---|---|---|
| `SshBruteForceDetector` | `/var/log/auth.log` | 5 failed logins from one IP within 60 s. A successful login right after that raises a CRITICAL alert. |
| `PortScanDetector` | Suricata `eve.json` (flow events) | 15 distinct destination ports from one IP within 10 s |
| `WebAttackDetector` | Apache / nginx access log | SQL injection, XSS and path traversal patterns in the decoded request, plus scanner user agents (sqlmap, nikto, ...) |
| `ArpSpoofDetector` | `ip neigh` or `arp -an` dumps | An IP address that suddenly maps to a different MAC |
| `DnsExfiltrationDetector` | `dnsmasq` / BIND / Suricata DNS | Abnormally long subdomain query labels (> 32 chars) or burst tunneling query volume |

Thresholds are constants at the top of each detector class. Tune them against your own baseline traffic.

## Layout

```
src/minisoc/
  Main.java                  entry point, wires CLI or GUI mode
  Detector.java              interface every detector implements
  Alert.java                 one finding, serialised as a JSON line
  AlertSink.java             console + alerts.json output, de-duplication
  LogTailer.java             tail -f style file reader (or one-shot replay)
  SshBruteForceDetector.java
  PortScanDetector.java
  WebAttackDetector.java
  ArpSpoofDetector.java
  DnsExfiltrationDetector.java
  gui/                       SOC Analyst GUI Dashboard (pure Java Swing, 0 deps)
    SocGui.java              main GUI dashboard application window
    SocEngine.java           engine controller coordinating tailers & detectors
    SocTheme.java            cyber SOC dark slate theme & custom components
    AlertsTableModel.java    filterable & sortable alerts table model
    AlertInspectorPanel.java alert drilldown & threat playbook response guidance
    AttackSimulator.java     interactive live attack traffic injector
    AttackSimulatorDialog.java simulated attack selection dialog
    ChartsPanel.java         severity stacked bar, attack category & attacker rank
    LiveLogPanel.java        real-time hacker terminal log stream
    DetectorsPanel.java      detector module rule descriptions & live toggles
    LogSourcesDialog.java    log source path manager & file chooser
    MetricCard.java          KPI stat card widget
    SeverityCellRenderer.java custom colored pill badges
samples/                     small logs with known attacks for testing
```

## Build and run

Compile all sources (zero external dependencies, standard JDK):

```bash
# On Linux / macOS
mkdir -p out
javac -d out src/minisoc/*.java src/minisoc/gui/*.java

# On Windows PowerShell
mkdir -Force out
javac -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
```

### Launch the SOC GUI Dashboard

Launch the interactive dark-mode Security Operations Center dashboard:

```bash
java -cp out minisoc.Main
# or explicitly:
java -cp out minisoc.Main --gui
# or direct class:
java -cp out minisoc.gui.SocGui
```

**GUI Highlights:**
- **KPI Stat Cards**: Real-time counter of total alerts, critical threats, unique attacker IPs, and log events parsed.
- **Interactive Alerts Table**: Color-coded pill badges (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), sorting, instant keyword search, and severity filters.
- **Alert Inspector & Playbook Guidance**: Detailed breakdown of selected alert with offending IP copy/filter actions, raw JSON viewer, and actionable SOC analyst remediation instructions.
- **Threat Analytics & Charts**: Pure Java2D charts for severity distribution, attack categories, and top offending source IPs.
- **Live Log Stream Terminal**: Real-time auto-scrolling terminal console displaying tailed log lines across all sources.
- **Sensor Rules Manager**: View detection thresholds and toggle individual detectors on the fly.
- **1-Click Lab Replay**: Instant button to test against the 4 sample attack logs.
- **Log Source Configuration**: File chooser to add any custom log file or adjust alert cooldown.
- **Export**: Save alerts to JSON Lines or CSV format.

### Command-line (CLI) Mode

Test against the sample logs (reads each file once and exits):

```bash
java -cp out minisoc.Main --replay samples/auth.log samples/access.log samples/eve.json samples/arp.log
```

You should see 8 alerts: one each for SSH brute force, login after brute force, port scan, SQL injection, XSS, path traversal, scanner tool, and ARP spoofing.

Live mode on the sensor VM (follows the files like `tail -f`):

```bash
java -cp out minisoc.Main --out /var/log/minisoc/alerts.json \
    /var/log/auth.log /var/log/apache2/access.log /var/log/suricata/eve.json /var/log/arp.log
```

For the ARP detector, keep a neighbour table log growing in the background:

```bash
while true; do ip neigh >> /var/log/arp.log; sleep 5; done
```

## Sending alerts to the dashboard

Each alert is one JSON object per line:

```json
{"timestamp":"2026-11-08T12:00:05Z","type":"port_scan","severity":"HIGH","source_ip":"192.168.56.10","detail":"touched 15 different ports in 10 seconds"}
```

Point Filebeat at the alerts file and parse it as JSON, then build a Kibana dashboard on `type`, `severity` and `source_ip`.

## Alert de-duplication

The same alert type from the same source IP is only reported once every 30 seconds (`ALERT_COOLDOWN_MS` in `Main.java`), otherwise a single nmap scan would produce hundreds of alerts.

## Known limitations

- Log rotation that replaces the file is not followed; restart the tool after rotation (truncation is handled).
- ARP changes caused by legitimate events (new NIC, DHCP reuse) look the same as spoofing, so expect some false positives.
- Detection is pattern based, so encoded or obfuscated payloads beyond double URL-encoding can slip through.
- The port scan rule uses Suricata flow events, which are written when a flow ends, so alerts can lag the scan by a few seconds.

## Ideas for extending it

- A DNS exfiltration detector as a fifth module (implement `Detector`, register it in `Main`).
- Per-detector configuration through a properties file instead of constants.
- A Cowrie honeypot log as another input.
- Unit tests with JUnit for each detector.
