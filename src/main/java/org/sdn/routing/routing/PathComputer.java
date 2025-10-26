package org.sdn.routing.routing;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.sdn.routing.config.Configuration;
import org.sdn.routing.model.Link;
import org.sdn.routing.model.LinkUtilization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes alternative paths in the network topology
 */
public class PathComputer {
    private static final Logger logger = LoggerFactory.getLogger(PathComputer.class);

    private final Configuration config;
    private Graph<String, DefaultWeightedEdge> graph;

    public PathComputer(Configuration config) {
        this.config = config;
        this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    }

    /**
     * Update the network topology graph
     */
    public void updateTopology(List<Link> links) {
        // Rebuild graph from scratch
        graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // Add all vertices (devices)
        Set<String> devices = new HashSet<>();
        for (Link link : links) {
            devices.add(link.getSrc().getDevice());
            devices.add(link.getDst().getDevice());
        }

        for (String device : devices) {
            graph.addVertex(device);
        }

        // Add all edges (links) with default weight
        for (Link link : links) {
            if (link.getState().equals("ACTIVE")) {
                String src = link.getSrc().getDevice();
                String dst = link.getDst().getDevice();

                if (!graph.containsEdge(src, dst)) {
                    DefaultWeightedEdge edge = graph.addEdge(src, dst);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, 1.0); // Default weight
                    }
                }
            }
        }

        logger.info("Topology updated: {} devices, {} links",
            graph.vertexSet().size(), graph.edgeSet().size());
    }

    /**
     * Find K shortest paths between source and destination
     */
    public List<NetworkPath> findKShortestPaths(String srcDevice, String dstDevice,
                                                Map<String, LinkUtilization> utilizations) {
        if (!graph.containsVertex(srcDevice) || !graph.containsVertex(dstDevice)) {
            logger.warn("Source or destination device not in topology");
            return Collections.emptyList();
        }

        // Update edge weights based on current utilization
        updateEdgeWeights(utilizations);

        // Use K-shortest paths algorithm (Yen's algorithm)
        YenKShortestPath<String, DefaultWeightedEdge> pathFinder =
            new YenKShortestPath<>(graph);

        List<GraphPath<String, DefaultWeightedEdge>> graphPaths =
            pathFinder.getPaths(srcDevice, dstDevice, config.getKPaths());

        // Convert to NetworkPath objects
        List<NetworkPath> paths = new ArrayList<>();
        for (GraphPath<String, DefaultWeightedEdge> gp : graphPaths) {
            NetworkPath path = new NetworkPath(gp.getVertexList(), gp.getWeight());
            paths.add(path);
        }

        logger.debug("Found {} paths from {} to {}", paths.size(), srcDevice, dstDevice);
        return paths;
    }

    /**
     * Find the best alternative path avoiding a congested link
     */
    public NetworkPath findBestAlternativePath(String srcDevice, String dstDevice,
                                              String congestedLinkId,
                                              Map<String, LinkUtilization> utilizations) {
        List<NetworkPath> paths = findKShortestPaths(srcDevice, dstDevice, utilizations);

        // Filter out paths that use the congested link
        List<NetworkPath> alternatePaths = paths.stream()
            .filter(path -> !pathUsesLink(path, congestedLinkId))
            .collect(Collectors.toList());

        if (alternatePaths.isEmpty()) {
            logger.warn("No alternative paths found avoiding {}", congestedLinkId);
            return null;
        }

        // Score paths and select the best
        NetworkPath bestPath = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (NetworkPath path : alternatePaths) {
            double score = scorePath(path, utilizations);
            if (score > bestScore) {
                bestScore = score;
                bestPath = path;
            }
        }

        logger.info("Best alternative path: {} (score: {:.2f})", bestPath, bestScore);
        return bestPath;
    }

    /**
     * Update edge weights based on link utilization
     */
    private void updateEdgeWeights(Map<String, LinkUtilization> utilizations) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            String src = graph.getEdgeSource(edge);
            String dst = graph.getEdgeTarget(edge);
            String linkId = src + "->" + dst;

            LinkUtilization util = utilizations.get(linkId);
            double weight;

            if (util != null) {
                // Weight increases with utilization
                // Higher utilization = higher cost
                double utilPercent = util.getUtilizationPercent();
                if (utilPercent >= config.getCongestionThreshold()) {
                    // Heavily penalize congested links
                    weight = 100.0;
                } else {
                    // Weight based on utilization (1 to 10)
                    weight = 1.0 + (utilPercent / 10.0);
                }
            } else {
                weight = 1.0; // Default weight for unknown links
            }

            graph.setEdgeWeight(edge, weight);
        }
    }

    /**
     * Check if a path uses a specific link
     */
    private boolean pathUsesLink(NetworkPath path, String linkId) {
        List<String> devices = path.getDevices();
        for (int i = 0; i < devices.size() - 1; i++) {
            String currentLink = devices.get(i) + "->" + devices.get(i + 1);
            if (currentLink.equals(linkId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Score a path based on various metrics
     * Higher score = better path
     */
    private double scorePath(NetworkPath path, Map<String, LinkUtilization> utilizations) {
        double score = 100.0; // Start with perfect score

        List<String> devices = path.getDevices();

        // Penalize longer paths
        score -= (devices.size() - 2) * 5.0; // -5 points per hop

        // Penalize based on link utilization
        for (int i = 0; i < devices.size() - 1; i++) {
            String linkId = devices.get(i) + "->" + devices.get(i + 1);
            LinkUtilization util = utilizations.get(linkId);

            if (util != null) {
                // Penalize high utilization
                score -= util.getUtilizationPercent() * 0.5;
            }
        }

        return score;
    }

    /**
     * Represents a path through the network
     */
    public static class NetworkPath {
        private final List<String> devices;
        private final double weight;

        public NetworkPath(List<String> devices, double weight) {
            this.devices = new ArrayList<>(devices);
            this.weight = weight;
        }

        public List<String> getDevices() {
            return devices;
        }

        public double getWeight() {
            return weight;
        }

        public int getHopCount() {
            return devices.size() - 1;
        }

        @Override
        public String toString() {
            return String.join(" -> ", devices) +
                   String.format(" (weight: %.2f, hops: %d)", weight, getHopCount());
        }
    }
}
