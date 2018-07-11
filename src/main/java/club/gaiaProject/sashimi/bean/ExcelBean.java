package club.gaiaProject.sashimi.bean;

import java.util.HashSet;
import java.util.Set;

public class ExcelBean {
    private String subway;
    private String deviceName;
    private Set<String> alarmInfo = new HashSet<>();
    private String alarmCount;
    private Long lastTime;

    public String getSubway() {
        return subway;
    }

    public void setSubway(String subway) {
        this.subway = subway;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Set<String> getAlarmInfo() {
        return alarmInfo;
    }

    public void setAlarmInfo(Set<String> alarmInfo) {
        this.alarmInfo = alarmInfo;
    }

    public String getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(String alarmCount) {
        this.alarmCount = alarmCount;
    }

    public Long getLastTime() {
        return lastTime;
    }

    public void setLastTime(Long lastTime) {
        this.lastTime = lastTime;
    }
}
