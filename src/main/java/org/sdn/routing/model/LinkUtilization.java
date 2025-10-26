package org.sdn.routing.model;

/**
 * Represents real-time utilization of a network link
 */
public class LinkUtilization {
    private String linkId;
    private String srcDevice;
    private String dstDevice;
    private double utilizationPercent;
    private long bytesPerSecond;
    private long timestamp;
    private boolean congested;

    public LinkUtilization(String linkId, String srcDevice, String dstDevice) {
        this.linkId = linkId;
        this.srcDevice = srcDevice;
        this.dstDevice = dstDevice;
        this.timestamp = System.currentTimeMillis();
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getSrcDevice() {
        return srcDevice;
    }

    public void setSrcDevice(String srcDevice) {
        this.srcDevice = srcDevice;
    }

    public String getDstDevice() {
        return dstDevice;
    }

    public void setDstDevice(String dstDevice) {
        this.dstDevice = dstDevice;
    }

    public double getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(double utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public long getBytesPerSecond() {
        return bytesPerSecond;
    }

    public void setBytesPerSecond(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCongested() {
        return congested;
    }

    public void setCongested(boolean congested) {
        this.congested = congested;
    }

    /**
     * Get throughput in Mbps
     */
    public double getMbps() {
        return (bytesPerSecond * 8.0) / (1024 * 1024);
    }

    @Override
    public String toString() {
        return String.format("LinkUtil{%s: %.1f%% (%.2f Mbps), congested=%s}",
            linkId, utilizationPercent, getMbps(), congested);
    }
}
