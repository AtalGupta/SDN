package org.sdn.routing;

import org.sdn.routing.api.OnosApiClient;
import org.sdn.routing.config.Configuration;
import org.sdn.routing.detection.CongestionDetector;
import org.sdn.routing.intent.IntentManager;
import org.sdn.routing.model.*;
import org.sdn.routing.monitor.TrafficMonitor;
import org.sdn.routing.routing.PathComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application - SDN Dynamic Routing with Congestion Detection
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private final Configuration config;
    private final OnosApiClient onosApi;
    private final TrafficMonitor trafficMonitor;
    private final CongestionDetector congestionDetector;
    private final PathComputer pathComputer;
    private final IntentManager intentManager;
    private final ScheduledExecutorService scheduler;

    public App() throws IOException {
        logger.info("Initializing SDN Dynamic Routing Application...");

        // Load configuration
        this.config = new Configuration();
        logger.info("Configuration loaded: {}", config);

        // Initialize components
        this.onosApi = new OnosApiClient(config);
        this.trafficMonitor = new TrafficMonitor(onosApi, config);
        this.congestionDetector = new CongestionDetector(config);
        this.pathComputer = new PathComputer(config);
        this.intentManager = new IntentManager(onosApi);
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Setup event listeners
        setupListeners();

        logger.info("Application initialized successfully");
    }

    /**
     * Setup event listeners between components
     */
    private void setupListeners() {
        // Traffic monitor notifies congestion detector
        trafficMonitor.addListener(new TrafficMonitor.TrafficListener() {
            @Override
            public void onTrafficUpdate(Map<String, LinkUtilization> utilizations) {
                // Analyze for congestion
                congestionDetector.analyzeUtilizations(utilizations);

                // Log high utilization links
                for (LinkUtilization util : utilizations.values()) {
                    if (util.getUtilizationPercent() > 50.0) {
                        logger.info("Link {}: {:.1f}% utilization ({:.2f} Mbps)",
                            util.getLinkId(),
                            util.getUtilizationPercent(),
                            util.getMbps());
                    }
                }
            }
        });

        // Congestion detector notifies for rerouting
        congestionDetector.addListener(new CongestionDetector.CongestionListener() {
            @Override
            public void onCongestionDetected(String linkId, LinkUtilization utilization) {
                logger.warn("CONGESTION DETECTED: {} at {:.1f}% ({:.2f} Mbps)",
                    linkId, utilization.getUtilizationPercent(), utilization.getMbps());

                // Trigger rerouting
                handleCongestion(linkId, utilization);
            }

            @Override
            public void onCongestionCleared(String linkId, LinkUtilization utilization) {
                logger.info("CONGESTION CLEARED: {} now at {:.1f}%",
                    linkId, utilization.getUtilizationPercent());
            }
        });
    }

    /**
     * Handle congestion by finding and installing alternative paths
     */
    private void handleCongestion(String congestedLinkId, LinkUtilization utilization) {
        logger.info("Attempting to reroute traffic around congested link: {}", congestedLinkId);

        try {
            // Update topology
            List<Link> links = onosApi.getLinks();
            pathComputer.updateTopology(links);

            // Get current utilizations
            Map<String, LinkUtilization> utilizations = trafficMonitor.getAllUtilizations();

            // Extract source and destination from congested link
            String srcDevice = utilization.getSrcDevice();
            String dstDevice = utilization.getDstDevice();

            // Find alternative path
            PathComputer.NetworkPath altPath = pathComputer.findBestAlternativePath(
                srcDevice, dstDevice, congestedLinkId, utilizations);

            if (altPath == null) {
                logger.warn("No alternative path found for congested link {}", congestedLinkId);
                return;
            }

            logger.info("Alternative path found: {}", altPath);

            // Get hosts to install intent
            List<Host> hosts = onosApi.getHosts();
            if (hosts.size() >= 2) {
                // For demonstration, reroute between first two hosts
                String srcHost = hosts.get(0).getId();
                String dstHost = hosts.get(hosts.size() - 1).getId();

                logger.info("Installing new intent from {} to {} via alternative path",
                    srcHost, dstHost);

                boolean success = intentManager.installPathIntent(srcHost, dstHost, altPath);

                if (success) {
                    logger.info("REROUTING SUCCESSFUL: Traffic redirected via alternative path");
                } else {
                    logger.error("REROUTING FAILED: Could not install new intent");
                }
            }

        } catch (Exception e) {
            logger.error("Error handling congestion: {}", e.getMessage(), e);
        }
    }

    /**
     * Start the application
     */
    public void start() {
        logger.info("Starting SDN Dynamic Routing Application...");

        // Check ONOS connectivity
        if (!onosApi.isAccessible()) {
            logger.error("Cannot connect to ONOS controller at {}", config.getOnosBaseUrl());
            logger.error("Please ensure ONOS is running and accessible");
            return;
        }

        logger.info("Successfully connected to ONOS at {}", config.getOnosBaseUrl());

        // Initialize topology
        try {
            List<Device> devices = onosApi.getDevices();
            List<Link> links = onosApi.getLinks();
            List<Host> hosts = onosApi.getHosts();

            logger.info("Network topology discovered:");
            logger.info("  Devices: {}", devices.size());
            logger.info("  Links: {}", links.size());
            logger.info("  Hosts: {}", hosts.size());

            if (devices.isEmpty()) {
                logger.warn("No devices found in topology!");
                logger.warn("Please start Mininet and ensure it's connected to ONOS");
            }

            // Update path computer topology
            pathComputer.updateTopology(links);

        } catch (IOException e) {
            logger.error("Error discovering topology: {}", e.getMessage());
        }

        // Schedule periodic traffic monitoring
        int interval = config.getMonitorPollingInterval();
        logger.info("Starting traffic monitor (polling every {} seconds)", interval);

        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    trafficMonitor.pollTraffic();
                } catch (Exception e) {
                    logger.error("Error polling traffic: {}", e.getMessage());
                }
            },
            0,
            interval,
            TimeUnit.SECONDS
        );

        logger.info("Application started successfully!");
        logger.info("Monitoring network traffic and detecting congestion...");
        logger.info("Press Ctrl+C to stop");
    }

    /**
     * Stop the application
     */
    public void stop() {
        logger.info("Stopping application...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        try {
            onosApi.close();
        } catch (IOException e) {
            logger.error("Error closing ONOS client: {}", e.getMessage());
        }

        logger.info("Application stopped");
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            App app = new App();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

            // Start application
            app.start();

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (IOException e) {
            logger.error("Failed to initialize application: {}", e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Application interrupted");
        }
    }
}
