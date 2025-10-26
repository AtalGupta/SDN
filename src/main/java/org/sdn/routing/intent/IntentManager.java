package org.sdn.routing.intent;

import org.sdn.routing.api.OnosApiClient;
import org.sdn.routing.routing.PathComputer.NetworkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Manages installation and removal of intents in ONOS
 */
public class IntentManager {
    private static final Logger logger = LoggerFactory.getLogger(IntentManager.class);

    private final OnosApiClient onosApi;

    public IntentManager(OnosApiClient onosApi) {
        this.onosApi = onosApi;
    }

    /**
     * Install an intent to route traffic along a specific path
     */
    public boolean installPathIntent(String srcHostId, String dstHostId, NetworkPath path) {
        logger.info("Installing intent: {} -> {} via {}",
            srcHostId, dstHostId, path.getDevices());

        try {
            boolean success = onosApi.installIntent(srcHostId, dstHostId, path.getDevices());

            if (success) {
                logger.info("Intent installed successfully");
            } else {
                logger.error("Failed to install intent");
            }

            return success;

        } catch (IOException e) {
            logger.error("Error installing intent: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Install a simple host-to-host intent (ONOS picks the path)
     */
    public boolean installHostIntent(String srcHostId, String dstHostId) {
        logger.info("Installing host-to-host intent: {} -> {}", srcHostId, dstHostId);

        try {
            boolean success = onosApi.installIntent(srcHostId, dstHostId, null);

            if (success) {
                logger.info("Host intent installed successfully");
            } else {
                logger.error("Failed to install host intent");
            }

            return success;

        } catch (IOException e) {
            logger.error("Error installing host intent: {}", e.getMessage());
            return false;
        }
    }
}
