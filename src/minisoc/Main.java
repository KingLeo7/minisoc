package minisoc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point. Every log file you pass in gets its own tailer thread,
 * and every line from every file is offered to every detector.
 */
public class Main {

    private static final long ALERT_COOLDOWN_MS = 30_000;

    private static final List<Detector> detectors = new ArrayList<>();
    private static AlertSink sink;

    public static void main(String[] args) throws InterruptedException {
        // If no arguments or --gui flag provided, launch GUI dashboard
        if (args.length == 0 || (args.length == 1 && args[0].equals("--gui"))) {
            minisoc.gui.SocGui.main(args);
            return;
        }

        boolean replay = false;
        String outFile = "alerts.json";
        List<Path> logs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--replay")) {
                replay = true;
            } else if (arg.equals("--out")) {
                if (i + 1 >= args.length) {
                    printUsage();
                    return;
                }
                outFile = args[++i];
            } else if (arg.equals("--help")) {
                printUsage();
                return;
            } else {
                logs.add(Paths.get(arg));
            }
        }

        if (logs.isEmpty()) {
            printUsage();
            return;
        }

        sink = new AlertSink(Paths.get(outFile), ALERT_COOLDOWN_MS);
        detectors.add(new SshBruteForceDetector());
        detectors.add(new PortScanDetector());
        detectors.add(new WebAttackDetector());
        detectors.add(new ArpSpoofDetector());
        detectors.add(new DnsExfiltrationDetector());
        detectors.add(new CowrieHoneypotDetector());

        System.out.println("MiniSOC started, " + detectors.size() + " detectors, "
                + logs.size() + " log file(s), alerts -> " + outFile
                + (replay ? " (replay mode)" : ""));

        List<Thread> threads = new ArrayList<>();
        for (Path log : logs) {
            Thread t = new Thread(new LogTailer(log, replay, Main::handleLine),
                    "tail-" + log.getFileName());
            t.start();
            threads.add(t);
        }

        if (replay) {
            for (Thread t : threads) {
                t.join();
            }
            sink.printSummary();
        }
    }

    // several tailer threads call this, so only one line is processed at a time
    private static synchronized void handleLine(String line) {
        for (Detector detector : detectors) {
            for (Alert alert : detector.inspect(line)) {
                sink.emit(alert);
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp out minisoc.Main [--gui] [--replay] [--out alerts.json] <logfile>...");
        System.out.println();
        System.out.println("  --gui        launch the interactive SOC Analyst GUI dashboard (default if no args)");
        System.out.println("  --replay     read each file once from the start and exit (for testing)");
        System.out.println("  --out FILE   where to append JSON alerts (default: alerts.json)");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -cp out minisoc.Main --gui");
        System.out.println("  java -cp out minisoc.Main /var/log/auth.log /var/log/apache2/access.log"
                + " /var/log/suricata/eve.json arp.log");
    }
}
