package minisoc.gui;

import minisoc.Alert;

import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Table model for detected alerts with real-time multi-criteria filtering.
 */
public class AlertsTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Timestamp", "Severity", "Attack Type", "Source IP", "Detail"};
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final List<Alert> allAlerts = new ArrayList<>();
    private final List<Alert> displayedAlerts = new ArrayList<>();

    private String searchFilter = "";
    private String severityFilter = "ALL";
    private String typeFilter = "ALL";
    private String ipFilter = "";

    @Override
    public int getRowCount() {
        return displayedAlerts.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= displayedAlerts.size()) return "";
        Alert alert = displayedAlerts.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return TIME_FORMATTER.format(alert.getTime());
            case 1:
                return alert.getSeverity();
            case 2:
                return alert.getType();
            case 3:
                return alert.getSourceIp();
            case 4:
                return alert.getDetail();
            default:
                return "";
        }
    }

    public synchronized void addAlert(Alert alert) {
        allAlerts.add(alert);
        if (matchesFilter(alert)) {
            int newRow = displayedAlerts.size();
            displayedAlerts.add(alert);
            fireTableRowsInserted(newRow, newRow);
        }
    }

    public synchronized void setAlerts(List<Alert> alerts) {
        allAlerts.clear();
        allAlerts.addAll(alerts);
        applyFilters();
    }

    public synchronized void clear() {
        allAlerts.clear();
        displayedAlerts.clear();
        fireTableDataChanged();
    }

    public synchronized Alert getAlertAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < displayedAlerts.size()) {
            return displayedAlerts.get(rowIndex);
        }
        return null;
    }

    public synchronized List<Alert> getAllAlerts() {
        return new ArrayList<>(allAlerts);
    }

    public synchronized List<Alert> getDisplayedAlerts() {
        return new ArrayList<>(displayedAlerts);
    }

    public synchronized void setSearchFilter(String query) {
        this.searchFilter = query == null ? "" : query.trim().toLowerCase();
        applyFilters();
    }

    public synchronized void setSeverityFilter(String severity) {
        this.severityFilter = severity == null ? "ALL" : severity.trim().toUpperCase();
        applyFilters();
    }

    public synchronized void setTypeFilter(String type) {
        this.typeFilter = type == null ? "ALL" : type.trim();
        applyFilters();
    }

    public synchronized void setIpFilter(String ip) {
        this.ipFilter = ip == null ? "" : ip.trim();
        applyFilters();
    }

    public synchronized void resetFilters() {
        this.searchFilter = "";
        this.severityFilter = "ALL";
        this.typeFilter = "ALL";
        this.ipFilter = "";
        applyFilters();
    }

    public synchronized String getSeverityFilter() {
        return severityFilter;
    }

    public synchronized String getIpFilter() {
        return ipFilter;
    }

    private void applyFilters() {
        displayedAlerts.clear();
        for (Alert a : allAlerts) {
            if (matchesFilter(a)) {
                displayedAlerts.add(a);
            }
        }
        fireTableDataChanged();
    }

    private boolean matchesFilter(Alert a) {
        if (!"ALL".equalsIgnoreCase(severityFilter) && !a.getSeverity().equalsIgnoreCase(severityFilter)) {
            return false;
        }

        if (!"ALL".equalsIgnoreCase(typeFilter) && !a.getType().equalsIgnoreCase(typeFilter)) {
            return false;
        }

        if (!ipFilter.isEmpty() && !a.getSourceIp().equalsIgnoreCase(ipFilter)) {
            return false;
        }

        if (!searchFilter.isEmpty()) {
            boolean matched = a.getType().toLowerCase().contains(searchFilter)
                    || a.getSourceIp().toLowerCase().contains(searchFilter)
                    || a.getDetail().toLowerCase().contains(searchFilter)
                    || a.getSeverity().toLowerCase().contains(searchFilter);
            if (!matched) return false;
        }

        return true;
    }

    // Stats calculations
    public synchronized int getTotalCount() {
        return allAlerts.size();
    }

    public synchronized int getCountBySeverity(String severity) {
        int count = 0;
        for (Alert a : allAlerts) {
            if (a.getSeverity().equalsIgnoreCase(severity)) count++;
        }
        return count;
    }

    public synchronized int getUniqueIpCount() {
        Set<String> ips = new HashSet<>();
        for (Alert a : allAlerts) {
            ips.add(a.getSourceIp());
        }
        return ips.size();
    }

    public synchronized Map<String, Integer> getTypeCounts() {
        Map<String, Integer> map = new TreeMap<>();
        for (Alert a : allAlerts) {
            map.merge(a.getType(), 1, Integer::sum);
        }
        return map;
    }

    public synchronized Map<String, Integer> getIpCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Alert a : allAlerts) {
            map.merge(a.getSourceIp(), 1, Integer::sum);
        }
        // Sort descending by count
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}

