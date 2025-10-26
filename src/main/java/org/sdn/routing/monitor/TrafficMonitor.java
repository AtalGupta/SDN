package org.sdn.routing.monitor;

import org.sdn.routing.api.OnosApiClient;
import org.sdn.routing.config.Configuration;
import org.sdn.routing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors network traffic by polling ONOS for port statistics
 * and calculating link utilization
 */
public class TrafficMonitor {
    private static final Logger logger = LoggerFactory.getLogger(TrafficMonitor.class);

    private final OnosApiClient onosApi;
    private final Configuration config;
    private final Map<String, PortSnapshot> previousSnapshots;
    private final Map<String, LinkUtilization> currentUtilizations;
    private final List<TrafficListener> listeners;

    public TrafficMonitor(OnosApiClient onosApi, Configuration config) {
        this.onosApi = onosApi;
        this.config = config;
        this.previousSnapshots = new ConcurrentHashMap<>();
        this.currentUtilizations = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * Add a listener to be notified of traffic updates
     */
    public void addListener(TrafficListener listener) {
        listeners.add(listener);
    }

    /**
     * Poll ONOS for current traffic statistics and update utilizations
     */
    public void pollTraffic() {
        try {
            // Get current topology
            List<Device> devices = onosApi.getDevices();
            List<Link> links = onosApi.getLinks();

            // Build link map for quick lookup
            Map<String, Link> linkMap = buildLinkMap(links);

            // Poll statistics for each device
            for (Device device : devices) {
                if (!device.isAvailable()) {
                    continue;
                }

                try {
                    List<PortStatistics> stats = onosApi.getPortStatistics(device.getId());
                    processPortStatistics(device.getId(), stats, linkMap);
                } catch (IOException e) {
                    logger.warn("Failed to get stats for device {}: {}",
                        device.getId(), e.getMessage());
                }
            }

            // Notify listeners of updated utilizations
            notifyListeners();

        } catch (IOException e) {
            logger.error("Failed to poll traffic: {}", e.getMessage());
        }
    }

    /**
     * Process port statistics for a device
     */
    private void processPortStatistics(String deviceId, List<PortStatistics> stats,
                                      Map<String, Link> linkMap) {
        long currentTime = System.currentTimeMillis();

        for (PortStatistics stat : stats) {
            String portKey = deviceId + ":" + stat.getPort();

            // Get previous snapshot
            PortSnapshot previous = previousSnapshots.get(portKey);

            // Store current snapshot
            PortSnapshot current = new PortSnapshot(
                stat.getBytesSent(),
                stat.getBytesReceived(),
                currentTime
            );
            previousSnapshots.put(portKey, current);

            // Calculate utilization if we have a previous snapshot
            if (previous != null) {
                calculateUtilization(deviceId, stat.getPort(), previous, current, linkMap);
            }
        }
    }

    /**
     * Calculate link utilization from port statistics
     */
    private void calculateUtilization(String deviceId, String port,
                                      PortSnapshot previous, PortSnapshot current,
                                      Map<String, Link> linkMap) {
        // Find the link for this port
        String linkKey = deviceId + ":" + port;
        Link link = linkMap.get(linkKey);

        if (link == null) {
            return; // Not a link port (might be host-facing)
        }

        // Calculate bytes transferred in the interval
        long bytesDelta = (current.bytesSent - previous.bytesSent) +
                         (current.bytesReceived - previous.bytesReceived);
        long timeDeltaMs = current.timestamp - previous.timestamp;

        if (timeDeltaMs <= 0) {
            return; // Invalid time delta
        }

        // Calculate throughput in bytes per second
        long bytesPerSecond = (bytesDelta * 1000) / timeDeltaMs;

        // Calculate utilization percentage
        long linkCapacity = config.getLinkCapacityBps();
        double utilizationPercent = (bytesPerSecond * 8.0 * 100.0) / linkCapacity;

        // Create or update LinkUtilization object
        String linkId = link.getLinkId();
        LinkUtilization util = currentUtilizations.computeIfAbsent(
            linkId,
            k -> new LinkUtilization(
                linkId,
                link.getSrc().getDevice(),
                link.getDst().getDevice()
            )
        );

        util.setBytesPerSecond(bytesPerSecond);
        util.setUtilizationPercent(utilizationPercent);
        util.setTimestamp(current.timestamp);

        logger.debug("Link {}: {:.1f}% ({} Mbps)",
            linkId, utilizationPercent, util.getMbps());
    }

    /**
     * Build a map of links indexed by source device and port
     */
    private Map<String, Link> buildLinkMap(List<Link> links) {
        Map<String, Link> map = new HashMap<>();
        for (Link link : links) {
            String key = link.getSrc().getDevice() + ":" + link.getSrc().getPort();
            map.put(key, link);
        }
        return map;
    }

    /**
     * Notify all listeners of updated traffic information
     */
    private void notifyListeners() {
        for (TrafficListener listener : listeners) {
            try {
                listener.onTrafficUpdate(new HashMap<>(currentUtilizations));
            } catch (Exception e) {
                logger.error("Error notifying listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Get current utilization for a specific link
     */
    public LinkUtilization getUtilization(String linkId) {
        return currentUtilizations.get(linkId);
    }

    /**
     * Get all current link utilizations
     */
    public Map<String, LinkUtilization> getAllUtilizations() {
        return new HashMap<>(currentUtilizations);
    }

    /**
     * Interface for traffic update listeners
     */
    public interface TrafficListener {
        void onTrafficUpdate(Map<String, LinkUtilization> utilizations);
    }

    /**
     * Snapshot of port statistics at a point in time
     */
    private static class PortSnapshot {
        final long bytesSent;
        final long bytesReceived;
        final long timestamp;

        PortSnapshot(long bytesSent, long bytesReceived, long timestamp) {
            this.bytesSent = bytesSent;
            this.bytesReceived = bytesReceived;
            this.timestamp = timestamp;
        }
    }
}
