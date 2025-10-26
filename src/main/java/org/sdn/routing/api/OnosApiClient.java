package org.sdn.routing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.sdn.routing.config.Configuration;
import org.sdn.routing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API client for ONOS controller
 */
public class OnosApiClient {
    private static final Logger logger = LoggerFactory.getLogger(OnosApiClient.class);
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public OnosApiClient(Configuration config) {
        this.baseUrl = config.getOnosBaseUrl();

        // Setup HTTP client with basic authentication
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            new AuthScope(config.getOnosIp(), config.getOnosPort()),
            new UsernamePasswordCredentials(
                config.getOnosUser(),
                config.getOnosPassword().toCharArray()
            )
        );

        this.httpClient = HttpClients.custom()
            .setDefaultCredentialsProvider(credentialsProvider)
            .build();

        this.objectMapper = new ObjectMapper();

        logger.info("ONOS API client initialized: {}", baseUrl);
    }

    /**
     * Get all devices in the topology
     */
    public List<Device> getDevices() throws IOException {
        String url = baseUrl + "/devices";
        String response = executeGet(url);

        JsonNode root = objectMapper.readTree(response);
        JsonNode devicesNode = root.get("devices");

        List<Device> devices = new ArrayList<>();
        if (devicesNode != null && devicesNode.isArray()) {
            for (JsonNode node : devicesNode) {
                Device device = objectMapper.treeToValue(node, Device.class);
                devices.add(device);
            }
        }

        logger.debug("Retrieved {} devices", devices.size());
        return devices;
    }

    /**
     * Get all links in the topology
     */
    public List<Link> getLinks() throws IOException {
        String url = baseUrl + "/links";
        String response = executeGet(url);

        JsonNode root = objectMapper.readTree(response);
        JsonNode linksNode = root.get("links");

        List<Link> links = new ArrayList<>();
        if (linksNode != null && linksNode.isArray()) {
            for (JsonNode node : linksNode) {
                Link link = objectMapper.treeToValue(node, Link.class);
                links.add(link);
            }
        }

        logger.debug("Retrieved {} links", links.size());
        return links;
    }

    /**
     * Get all hosts in the network
     */
    public List<Host> getHosts() throws IOException {
        String url = baseUrl + "/hosts";
        String response = executeGet(url);

        JsonNode root = objectMapper.readTree(response);
        JsonNode hostsNode = root.get("hosts");

        List<Host> hosts = new ArrayList<>();
        if (hostsNode != null && hostsNode.isArray()) {
            for (JsonNode node : hostsNode) {
                Host host = objectMapper.treeToValue(node, Host.class);
                hosts.add(host);
            }
        }

        logger.debug("Retrieved {} hosts", hosts.size());
        return hosts;
    }

    /**
     * Get port statistics for a specific device
     */
    public List<PortStatistics> getPortStatistics(String deviceId) throws IOException {
        String url = baseUrl + "/statistics/ports/" + deviceId;
        String response = executeGet(url);

        JsonNode root = objectMapper.readTree(response);
        JsonNode statsNode = root.get("statistics");

        List<PortStatistics> stats = new ArrayList<>();
        if (statsNode != null && statsNode.isArray()) {
            for (JsonNode node : statsNode) {
                PortStatistics stat = objectMapper.treeToValue(node, PortStatistics.class);
                stats.add(stat);
            }
        }

        return stats;
    }

    /**
     * Install a host-to-host intent for traffic routing
     */
    public boolean installIntent(String srcHostId, String dstHostId,
                                 List<String> pathDevices) throws IOException {
        String url = baseUrl + "/intents";

        // Build intent JSON
        JsonNode intentJson = buildHostToHostIntent(srcHostId, dstHostId, pathDevices);
        String requestBody = objectMapper.writeValueAsString(intentJson);

        logger.info("Installing intent: {} -> {} via {}",
            srcHostId, dstHostId, pathDevices);

        try {
            executePost(url, requestBody);
            return true;
        } catch (IOException e) {
            logger.error("Failed to install intent", e);
            return false;
        }
    }

    /**
     * Build a host-to-host intent JSON object
     */
    private JsonNode buildHostToHostIntent(String srcHostId, String dstHostId,
                                           List<String> pathDevices) {
        ObjectMapper mapper = new ObjectMapper();
        var intent = mapper.createObjectNode();

        intent.put("type", "HostToHostIntent");
        intent.put("appId", "org.sdn.routing");
        intent.put("priority", 100);
        intent.put("one", srcHostId);
        intent.put("two", dstHostId);

        // Add path constraints if provided
        if (pathDevices != null && !pathDevices.isEmpty()) {
            var constraints = intent.putArray("constraints");
            var pathConstraint = constraints.addObject();
            pathConstraint.put("type", "PathViabilityConstraint");
        }

        return intent;
    }

    /**
     * Execute HTTP GET request
     */
    private String executeGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String body;
            try {
                body = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                throw new IOException("Failed to parse response", e);
            }

            if (statusCode >= 200 && statusCode < 300) {
                return body;
            } else {
                throw new IOException("HTTP " + statusCode + ": " + body);
            }
        }
    }

    /**
     * Execute HTTP POST request
     */
    private String executePost(String url, String body) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                throw new IOException("Failed to parse response", e);
            }

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
        }
    }

    /**
     * Check if ONOS is accessible
     */
    public boolean isAccessible() {
        try {
            getDevices();
            return true;
        } catch (IOException e) {
            logger.error("ONOS not accessible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Close the HTTP client
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
