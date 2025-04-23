package org.plugin.bantools;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class BanEntry {
    private String name;
    private String uuid;
    private String ip;
    private String reason;
    private long startTime;
    private Long endTime;
    private boolean state = true;

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

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public boolean getState() { return state; }
    public void setState(boolean state) { this.state = state; }

    public boolean isPermanent() { return endTime == null; }
    public String getEndTimeFormatted() {
        if (isPermanent()) return "永久封禁";
        return DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(Instant.ofEpochMilli(endTime));
    }
}