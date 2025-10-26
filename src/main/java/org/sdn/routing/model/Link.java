package org.sdn.routing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a network link between two switches
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Link {
    @JsonProperty("src")
    private EndPoint src;

    @JsonProperty("dst")
    private EndPoint dst;

    @JsonProperty("type")
    private String type;

    @JsonProperty("state")
    private String state;

    public Link() {}

    public EndPoint getSrc() {
        return src;
    }

    public void setSrc(EndPoint src) {
        this.src = src;
    }

    public EndPoint getDst() {
        return dst;
    }

    public void setDst(EndPoint dst) {
        this.dst = dst;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * Get link identifier as "srcDevice->dstDevice"
     */
    public String getLinkId() {
        return src.getDevice() + "->" + dst.getDevice();
    }

    @Override
    public String toString() {
        return "Link{" + getLinkId() + ", state='" + state + "'}";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EndPoint {
        @JsonProperty("device")
        private String device;

        @JsonProperty("port")
        private String port;

        public EndPoint() {}

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return device + ":" + port;
        }
    }
}
