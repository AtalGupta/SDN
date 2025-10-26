package org.sdn.routing.detection;

import org.sdn.routing.config.Configuration;
import org.sdn.routing.model.LinkUtilization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects congestion on network links based on utilization thresholds
 */
public class CongestionDetector {
    private static final Logger logger = LoggerFactory.getLogger(CongestionDetector.class);

    private final Configuration config;
    private final Map<String, CongestionState> linkStates;
    private final List<CongestionListener> listeners;

    public CongestionDetector(Configuration config) {
        this.config = config;
        this.linkStates = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * Add a listener to be notified of congestion events
     */
    public void addListener(CongestionListener listener) {
        listeners.add(listener);
    }

    /**
     * Analyze current link utilizations and detect congestion
     */
    public void analyzeUtilizations(Map<String, LinkUtilization> utilizations) {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, LinkUtilization> entry : utilizations.entrySet()) {
            String linkId = entry.getKey();
            LinkUtilization util = entry.getValue();

            analyzeLink(linkId, util, currentTime);
        }
    }

    /**
     * Analyze a single link for congestion
     */
    private void analyzeLink(String linkId, LinkUtilization util, long currentTime) {
        CongestionState state = linkStates.computeIfAbsent(
            linkId,
            k -> new CongestionState()
        );

        double utilization = util.getUtilizationPercent();
        double threshold = config.getCongestionThreshold();
        double hysteresis = config.getCongestionHysteresis();
        long minDuration = config.getCongestionMinimumDuration();

        if (utilization >= threshold) {
            // Link is above threshold
            if (!state.isAboveThreshold) {
                // Transitioned to above threshold
                state.isAboveThreshold = true;
                state.thresholdExceededTime = currentTime;
                logger.debug("Link {} above threshold: {:.1f}%", linkId, utilization);
            } else {
                // Already above threshold - check duration
                long duration = currentTime - state.thresholdExceededTime;
                if (duration >= minDuration && !state.isCongested) {
                    // Congestion confirmed
                    state.isCongested = true;
                    util.setCongested(true);

                    logger.warn("Congestion detected on link {}: {:.1f}% utilization ({:.2f} Mbps)",
                        linkId, utilization, util.getMbps());

                    notifyCongestionDetected(linkId, util);
                }
            }
        } else if (utilization < (threshold - hysteresis)) {
            // Link is below hysteresis threshold
            if (state.isAboveThreshold || state.isCongested) {
                // Congestion cleared
                if (state.isCongested) {
                    logger.info("Congestion cleared on link {}: {:.1f}% utilization",
                        linkId, utilization);

                    util.setCongested(false);
                    notifyCongestionCleared(linkId, util);
                }

                state.reset();
            }
        }

        // Update congestion flag in utilization object
        if (state.isCongested) {
            util.setCongested(true);
        }
    }

    /**
     * Notify listeners of detected congestion
     */
    private void notifyCongestionDetected(String linkId, LinkUtilization util) {
        for (CongestionListener listener : listeners) {
            try {
                listener.onCongestionDetected(linkId, util);
            } catch (Exception e) {
                logger.error("Error notifying listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Notify listeners of cleared congestion
     */
    private void notifyCongestionCleared(String linkId, LinkUtilization util) {
        for (CongestionListener listener : listeners) {
            try {
                listener.onCongestionCleared(linkId, util);
            } catch (Exception e) {
                logger.error("Error notifying listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Get all currently congested links
     */
    public List<String> getCongestedLinks() {
        List<String> congested = new ArrayList<>();
        for (Map.Entry<String, CongestionState> entry : linkStates.entrySet()) {
            if (entry.getValue().isCongested) {
                congested.add(entry.getKey());
            }
        }
        return congested;
    }

    /**
     * Check if a specific link is congested
     */
    public boolean isCongested(String linkId) {
        CongestionState state = linkStates.get(linkId);
        return state != null && state.isCongested;
    }

    /**
     * Interface for congestion event listeners
     */
    public interface CongestionListener {
        void onCongestionDetected(String linkId, LinkUtilization utilization);
        void onCongestionCleared(String linkId, LinkUtilization utilization);
    }

    /**
     * Tracks congestion state for a single link
     */
    private static class CongestionState {
        boolean isAboveThreshold = false;
        boolean isCongested = false;
        long thresholdExceededTime = 0;

        void reset() {
            isAboveThreshold = false;
            isCongested = false;
            thresholdExceededTime = 0;
        }
    }
}
