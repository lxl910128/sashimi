package club.gaiaProject.sashimi.bean;

import club.gaiaProject.sashimi.util.ExcelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;

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

    private Row row;//原始行

    private Integer id;//标示 ID

    private Integer tmpName;//从deviceName中抽取的临时名字

    public Integer getTmpName() {
        return tmpName;
    }

    public void setTmpName(Integer tmpName) {
        this.tmpName = tmpName;
    }

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

    public Row getRow() {
        return row;
    }

    public void setRow(Row row) {
        this.row = row;
    }

    public EventVO toEventVO() {
        EventVO ret = new EventVO();
        if (StringUtils.isNotEmpty(this.alarm.getInfo())) {
            ret.setInfo(this.alarm.getInfo());
        }else {
            ret.setInfo("");
        }
        if (StringUtils.isNotEmpty(this.alarm.getLevel())) {
            ret.setLevel(this.alarm.getLevel());
        }else {
            ret.setLevel("");
        }
        if (StringUtils.isNotEmpty(this.device.getName())) {
            ret.setName(this.device.getName());
        }else {
            ret.setName("");
        }
        if (StringUtils.isNotEmpty(this.alarm.getReason())) {
            ret.setReason(this.alarm.getReason());
        }else {
            ret.setReason("");
        }
        if (StringUtils.isNotEmpty(this.device.getSubway())) {
            ret.setSubway(this.device.getSubway());
        }else {
            ret.setSubway("");
        }
        if (StringUtils.isNotEmpty(this.device.getTypeName())) {
            ret.setType(this.device.getTypeName());
        }else {
            ret.setType("");
        }
        ret.setTime(ExcelUtils.dateTimeFormat.format(new Date(this.timeStamp)));
        return ret;
    }

    public EventBean(Row row){
        this.row= row;
    }
}
