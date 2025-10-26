package org.sdn.routing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a host (end device) connected to the network
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {
    @JsonProperty("id")
    private String id;

    @JsonProperty("mac")
    private String mac;

    @JsonProperty("ipAddresses")
    private String[] ipAddresses;

    @JsonProperty("locations")
    private Location[] locations;

    public Host() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String[] getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String[] ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public Location[] getLocations() {
        return locations;
    }

    public void setLocations(Location[] locations) {
        this.locations = locations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("elementId")
        private String elementId;

        @JsonProperty("port")
        private String port;

        public Location() {}

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
    }

    @Override
    public String toString() {
        String ip = (ipAddresses != null && ipAddresses.length > 0) ? ipAddresses[0] : "N/A";
        return "Host{id='" + id + "', mac='" + mac + "', ip='" + ip + "'}";
    }
}
