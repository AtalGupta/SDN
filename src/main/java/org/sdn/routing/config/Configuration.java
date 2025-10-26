package org.sdn.routing.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loader
 */
public class Configuration {
    private final Properties properties;

    // ONOS connection
    private final String onosIp;
    private final int onosPort;
    private final String onosUser;
    private final String onosPassword;

    // Monitoring settings
    private final int monitorPollingInterval;

    // Congestion detection
    private final double congestionThreshold;
    private final double congestionHysteresis;
    private final long congestionMinimumDuration;

    // Routing settings
    private final int kPaths;
    private final long linkCapacityBps;

    public Configuration() throws IOException {
        properties = new Properties();

        // Load from classpath
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            properties.load(input);
        }

        // Parse ONOS settings
        onosIp = properties.getProperty("onos.ip", "127.0.0.1");
        onosPort = Integer.parseInt(properties.getProperty("onos.port", "8181"));
        onosUser = properties.getProperty("onos.user", "onos");
        onosPassword = properties.getProperty("onos.password", "rocks");

        // Parse monitoring settings
        monitorPollingInterval = Integer.parseInt(
            properties.getProperty("monitor.polling.interval", "5"));

        // Parse congestion detection settings
        congestionThreshold = Double.parseDouble(
            properties.getProperty("congestion.threshold", "80.0"));
        congestionHysteresis = Double.parseDouble(
            properties.getProperty("congestion.hysteresis", "10.0"));
        congestionMinimumDuration = Long.parseLong(
            properties.getProperty("congestion.minimum.duration", "10000"));

        // Parse routing settings
        kPaths = Integer.parseInt(properties.getProperty("routing.k.paths", "3"));
        linkCapacityBps = Long.parseLong(
            properties.getProperty("link.capacity.bps", "104857600")); // 100 Mbps default
    }

    public String getOnosIp() {
        return onosIp;
    }

    public int getOnosPort() {
        return onosPort;
    }

    public String getOnosUser() {
        return onosUser;
    }

    public String getOnosPassword() {
        return onosPassword;
    }

    public String getOnosBaseUrl() {
        return String.format("http://%s:%d/onos/v1", onosIp, onosPort);
    }

    public int getMonitorPollingInterval() {
        return monitorPollingInterval;
    }

    public double getCongestionThreshold() {
        return congestionThreshold;
    }

    public double getCongestionHysteresis() {
        return congestionHysteresis;
    }

    public long getCongestionMinimumDuration() {
        return congestionMinimumDuration;
    }

    public int getKPaths() {
        return kPaths;
    }

    public long getLinkCapacityBps() {
        return linkCapacityBps;
    }

    @Override
    public String toString() {
        return String.format("Config{ONOS=%s:%d, polling=%ds, threshold=%.1f%%, k=%d}",
            onosIp, onosPort, monitorPollingInterval, congestionThreshold, kPaths);
    }
}
