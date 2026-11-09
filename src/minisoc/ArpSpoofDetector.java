package minisoc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps an IP -> MAC table built from neighbour table dumps and shouts
 * when an address suddenly belongs to a different MAC.
 *
 * It understands both output styles:
 *   ip neigh:  192.168.56.1 dev eth1 lladdr 08:00:27:aa:bb:cc REACHABLE
 *   arp -an:   ? (192.168.56.1) at 08:00:27:aa:bb:cc [ether] on eth1
 *
 * Caveat: a legitimate change (new network card, DHCP handing an old
 * address to another machine) looks the same, so expect the odd false
 * positive on busy networks.
 */
public class ArpSpoofDetector implements Detector {

    private static final Pattern IP = Pattern.compile("\\b(\\d{1,3}(?:\\.\\d{1,3}){3})\\b");
    private static final Pattern MAC =
            Pattern.compile("\\b([0-9a-fA-F]{2}(?::[0-9a-fA-F]{2}){5})\\b");
    private static final String BROADCAST = "ff:ff:ff:ff:ff:ff";

    private final Map<String, String> macByIp = new HashMap<>();

    @Override
    public String name() {
        return "arp-spoof";
    }

    @Override
    public void reset() {
        macByIp.clear();
    }

    @Override
    public List<Alert> inspect(String line) {
        Matcher ipMatch = IP.matcher(line);
        Matcher macMatch = MAC.matcher(line);
        if (!ipMatch.find() || !macMatch.find()) {
            return Collections.emptyList();
        }

        String ip = ipMatch.group(1);
        String mac = macMatch.group(1).toLowerCase();
        if (mac.equals(BROADCAST)) {
            return Collections.emptyList();
        }

        String oldMac = macByIp.put(ip, mac);
        if (oldMac == null || oldMac.equals(mac)) {
            return Collections.emptyList();
        }

        // the classic sign: the new MAC already owns some other address
        String detail = "address moved from MAC " + oldMac + " to " + mac;
        for (Map.Entry<String, String> entry : macByIp.entrySet()) {
            if (!entry.getKey().equals(ip) && entry.getValue().equals(mac)) {
                detail += ", and " + mac + " also owns " + entry.getKey();
                break;
            }
        }
        return List.of(new Alert("arp_spoofing", "HIGH", ip, detail));
    }
}
