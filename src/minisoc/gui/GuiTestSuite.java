package minisoc.gui;

import minisoc.Alert;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automated End-to-End Test Suite for MiniSOC GUI & Engine.
 */
public class GuiTestSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       MiniSOC Automated Test Suite              ");
        System.out.println("=================================================\n");

        runTest("1. GUI Instantiation & Layout Initialization", GuiTestSuite::testGuiInitialization);
        runTest("2. Sample Replay & 5 Detectors Verification", GuiTestSuite::testReplayAndAllDetectors);
        runTest("3. Table Multi-Criteria Filtering", GuiTestSuite::testTableFiltering);
        runTest("4. Alert Inspector & Playbook Guidance", GuiTestSuite::testAlertInspector);
        runTest("5. Live Attack Simulator & Real-time Sensor", GuiTestSuite::testLiveAttackSimulation);
        runTest("6. Export to JSON & CSV", GuiTestSuite::testExporting);
        runTest("7. Clear & Reset State", GuiTestSuite::testClearAndReset);

        System.out.println("\n=================================================");
        System.out.printf("RESULTS: %d PASSED, %d FAILED\n", testsPassed, testsFailed);
        System.out.println("=================================================");

        if (testsFailed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static void runTest(String name, TestRunnable test) {
        System.out.print("[TEST] " + name + " ... ");
        try {
            test.run();
            System.out.println("PASSED ✓");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED ✗");
            System.err.println("  -> Error: " + t.getMessage());
            t.printStackTrace(System.err);
            testsFailed++;
        }
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Exception;
    }

    private static void testGuiInitialization() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        SwingUtilities.invokeAndWait(() -> {
            SocGui gui = new SocGui();
            assertNotNull(gui, "GUI instance must not be null");
            assertTrue(gui.getWidth() > 800, "Window width should be initialized");
            assertTrue(gui.getHeight() > 600, "Window height should be initialized");
            gui.dispose();
            initialized.set(true);
        });
        assertTrue(initialized.get(), "GUI must initialize on Event Dispatch Thread");
    }

    private static void testReplayAndAllDetectors() throws Exception {
        SocEngine engine = new SocEngine();
        AlertsTableModel model = new AlertsTableModel();

        CountDownLatch latch = new CountDownLatch(10);
        engine.addAlertListener(alert -> {
            model.addAlert(alert);
            latch.countDown();
        });

        CountDownLatch finishedLatch = new CountDownLatch(1);
        engine.startReplay(finishedLatch::countDown);

        boolean finished = finishedLatch.await(8, TimeUnit.SECONDS);
        assertTrue(finished, "Replay did not finish within timeout");

        // Wait up to 2 seconds for all alerts to be dispatched
        latch.await(2, TimeUnit.SECONDS);

        int total = model.getTotalCount();
        assertTrue(total >= 10, "Expected at least 10 alerts from 5 sample logs, got " + total);

        // Verify all 5 detectors produced expected alerts
        assertTrue(model.getCountBySeverity("CRITICAL") >= 1, "Should have CRITICAL alert (login after brute force)");
        assertTrue(model.getCountBySeverity("HIGH") >= 5, "Should have at least 5 HIGH alerts");
        assertTrue(model.getCountBySeverity("MEDIUM") >= 3, "Should have at least 3 MEDIUM alerts");

        // Verify specific types
        List<Alert> alerts = model.getAllAlerts();
        assertAlertExists(alerts, "ssh_brute_force");
        assertAlertExists(alerts, "ssh_login_after_brute_force");
        assertAlertExists(alerts, "port_scan");
        assertAlertExists(alerts, "sql_injection");
        assertAlertExists(alerts, "xss_attempt");
        assertAlertExists(alerts, "path_traversal");
        assertAlertExists(alerts, "scanner_tool_detected");
        assertAlertExists(alerts, "arp_spoofing");
        assertAlertExists(alerts, "dns_exfiltration");
        assertAlertExists(alerts, "dns_tunneling_burst");
    }

    private static void testTableFiltering() {
        AlertsTableModel model = new AlertsTableModel();
        model.addAlert(new Alert("sql_injection", "HIGH", "192.168.56.10", "request: UNION SELECT"));
        model.addAlert(new Alert("ssh_login_after_brute_force", "CRITICAL", "192.168.56.10", "login as root"));
        model.addAlert(new Alert("xss_attempt", "MEDIUM", "192.168.56.11", "<script>alert(1)</script>"));

        assertEquals(3, model.getRowCount(), "Initial row count should be 3");

        // Test severity filter
        model.setSeverityFilter("CRITICAL");
        assertEquals(1, model.getRowCount(), "CRITICAL filter should return 1 row");
        assertEquals("CRITICAL", model.getAlertAt(0).getSeverity(), "Row severity must be CRITICAL");

        model.setSeverityFilter("HIGH");
        assertEquals(1, model.getRowCount(), "HIGH filter should return 1 row");

        // Test search query filter
        model.setSeverityFilter("ALL");
        model.setSearchFilter("script");
        assertEquals(1, model.getRowCount(), "Search 'script' should return 1 row");
        assertEquals("xss_attempt", model.getAlertAt(0).getType(), "Row should be xss_attempt");

        // Test IP filter
        model.resetFilters();
        model.setIpFilter("192.168.56.11");
        assertEquals(1, model.getRowCount(), "Filter by IP 192.168.56.11 should return 1 row");

        model.resetFilters();
        assertEquals(3, model.getRowCount(), "Reset filters should restore all 3 rows");
    }

    private static void testAlertInspector() throws Exception {
        AlertInspectorPanel inspector = new AlertInspectorPanel();
        Alert alert = new Alert("ssh_login_after_brute_force", "CRITICAL", "192.168.56.10",
                "login as 'root' succeeded right after 7 failures");

        SwingUtilities.invokeAndWait(() -> {
            inspector.setAlert(alert);
        });

        // Test JSON parsing
        Alert parsed = Alert.fromJson(alert.toJson());
        assertNotNull(parsed, "Alert.fromJson must successfully parse Alert.toJson()");
        assertEquals(alert.getType(), parsed.getType(), "Parsed type must match");
        assertEquals(alert.getSeverity(), parsed.getSeverity(), "Parsed severity must match");
        assertEquals(alert.getSourceIp(), parsed.getSourceIp(), "Parsed IP must match");
    }

    private static void testLiveAttackSimulation() throws Exception {
        SocEngine engine = new SocEngine();
        engine.setCooldownMs(0);
        AlertsTableModel model = new AlertsTableModel();

        CountDownLatch alertLatch = new CountDownLatch(1);
        engine.addAlertListener(a -> {
            if ("sql_injection".equals(a.getType())) {
                model.addAlert(a);
                alertLatch.countDown();
            }
        });

        // Start live sensor
        engine.startLive();
        Thread.sleep(800); // Give tailers time to attach to end of files

        // Simulate live SQL Injection
        AttackSimulator.simulateSqlInjection("192.168.56.99");

        boolean received = alertLatch.await(5, TimeUnit.SECONDS);
        engine.stop();

        assertTrue(received, "Live sensor should pick up simulated attack from access.log in real time");
        assertEquals(1, model.getRowCount(), "Model should contain the live injected alert");
        assertEquals("192.168.56.99", model.getAlertAt(0).getSourceIp(), "Attacker IP must match injected IP");
    }

    private static void testExporting() throws IOException {
        SocEngine engine = new SocEngine();
        List<Alert> testAlerts = List.of(
                new Alert("sql_injection", "HIGH", "192.168.56.10", "SELECT * FROM users"),
                new Alert("ssh_brute_force", "HIGH", "192.168.56.10", "5 failed logins")
        );

        Path jsonFile = Paths.get("test_out.json");
        Path csvFile = Paths.get("test_out.csv");

        try {
            engine.exportJson(jsonFile, testAlerts);
            assertTrue(Files.exists(jsonFile), "JSON export file must exist");
            assertTrue(Files.size(jsonFile) > 50, "JSON export file must have content");

            engine.exportCsv(csvFile, testAlerts);
            assertTrue(Files.exists(csvFile), "CSV export file must exist");
            assertTrue(Files.size(csvFile) > 50, "CSV export file must have content");
            List<String> lines = Files.readAllLines(csvFile);
            assertTrue(lines.get(0).startsWith("Timestamp,Severity"), "CSV must have header");
            assertEquals(3, lines.size(), "CSV must have header + 2 alert rows");
        } finally {
            Files.deleteIfExists(jsonFile);
            Files.deleteIfExists(csvFile);
        }
    }

    private static void testClearAndReset() {
        SocEngine engine = new SocEngine();
        AlertsTableModel model = new AlertsTableModel();
        model.addAlert(new Alert("sql_injection", "HIGH", "192.168.56.10", "payload"));
        assertEquals(1, model.getTotalCount(), "Model should have 1 alert");

        engine.clearAlerts();
        model.clear();

        assertEquals(0, model.getTotalCount(), "Total count should be 0 after clear");
        assertEquals(0, model.getRowCount(), "Row count should be 0 after clear");
    }

    // Helper assertions
    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAILED: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("FAILED: " + message);
    }

    private static void assertAlertExists(List<Alert> alerts, String type) {
        for (Alert a : alerts) {
            if (a.getType().equalsIgnoreCase(type)) return;
        }
        throw new AssertionError("Expected alert of type '" + type + "' not found in alerts list");
    }
}
