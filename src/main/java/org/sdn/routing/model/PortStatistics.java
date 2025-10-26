package org.sdn.routing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents port statistics from ONOS
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortStatistics {
    @JsonProperty("port")
    private String port;

    @JsonProperty("packetsReceived")
    private long packetsReceived;

    @JsonProperty("packetsSent")
    private long packetsSent;

    @JsonProperty("bytesReceived")
    private long bytesReceived;

    @JsonProperty("bytesSent")
    private long bytesSent;

    @JsonProperty("durationSec")
    private long durationSec;

    public PortStatistics() {}

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public long getPacketsReceived() {
        return packetsReceived;
    }

    public void setPacketsReceived(long packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public long getPacketsSent() {
        return packetsSent;
    }

    public void setPacketsSent(long packetsSent) {
        this.packetsSent = packetsSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public long getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(long durationSec) {
        this.durationSec = durationSec;
    }

    @Override
    public String toString() {
        return "PortStats{port='" + port + "', bytesRx=" + bytesReceived +
               ", bytesTx=" + bytesSent + "}";
    }
}
