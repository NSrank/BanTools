package org.plugin.bantools;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * 临时封禁记录实体类
 * 用于管理fakeban功能的临时封禁数据
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
     * 检查临时封禁是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }

    /**
     * 获取剩余时间（分钟）
     */
    public long getRemainingMinutes() {
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining / (1000 * 60));
    }

    /**
     * 获取格式化的结束时间
     */
    public String getEndTimeFormatted() {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                .format(Instant.ofEpochMilli(endTime));
    }

    /**
     * 获取格式化的剩余时间
     */
    public String getRemainingTimeFormatted() {
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            return "已过期";
        }
        
        long minutes = remaining / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
}
