package club.gaiaProject.sashimi.bean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class EventBean {
    private AlarmBean alarm;
    private DeviceBean device;

    private Long timeStamp;//告警时间
    private String handler;//告警确认人
    private String handlerInfo;//告警确认说明
    private Long handlerTime;//告警确认时间

    private Integer id;//标示 ID

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AlarmBean getAlarm() {
        return alarm;
    }

    public void setAlarm(AlarmBean alarm) {
        this.alarm = alarm;
    }

    public DeviceBean getDevice() {
        return device;
    }

    public void setDevice(DeviceBean device) {
        this.device = device;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getHandlerInfo() {
        return handlerInfo;
    }

    public void setHandlerInfo(String handlerInfo) {
        this.handlerInfo = handlerInfo;
    }

    public Long getHandlerTime() {
        return handlerTime;
    }

    public void setHandlerTime(Long handlerTime) {
        this.handlerTime = handlerTime;
    }
}
