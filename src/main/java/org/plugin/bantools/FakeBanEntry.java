package org.plugin.bantools;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Temporary ban record entity
 * Used to manage temporary ban data for the fakeban feature
 */
public class FakeBanEntry {
    private String name;
    private String uuid;
    private String ip;
    private String reason;
    private long startTime;
    private long endTime;
    private boolean state;

    public FakeBanEntry() {
        this.state = true;
        this.startTime = System.currentTimeMillis();
    }

    public FakeBanEntry(String name, String reason, long duration) {
        this();
        this.name = name;
        this.reason = reason;
        this.endTime = this.startTime + duration;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean getState() { return state; }
    public void setState(boolean state) { this.state = state; }

    /**
     * Check whether the temporary ban has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }

    /**
     * Get remaining time (minutes)
     */
    public long getRemainingMinutes() {
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining / (1000 * 60));
    }

    /**
     * Get formatted end time
     */
    public String getEndTimeFormatted() {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                .format(Instant.ofEpochMilli(endTime));
    }

    /**
     * Get formatted remaining time
     */
    public String getRemainingTimeFormatted() {
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            return "Expired";
        }
        
        long minutes = remaining / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }
}
