package club.gaiaProject.sashimi.bean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class AlarmBean {
    private String level;//告警等级
    private String type;//告警类别
    private String info;//告警信息
    private String reason;//告警原因

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
