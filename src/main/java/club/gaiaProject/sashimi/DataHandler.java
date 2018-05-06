package club.gaiaProject.sashimi;

import club.gaiaProject.sashimi.util.ExcelUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import club.gaiaProject.sashimi.bean.AlarmBean;
import club.gaiaProject.sashimi.bean.DeviceBean;
import club.gaiaProject.sashimi.bean.EventBean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class DataHandler {
    private List<EventBean> events;//execl 中的全部记录
    private Calendar calendar = Calendar.getInstance();
    //采集信息
    private List<EventBean> overhaulEvents = new ArrayList<EventBean>();//（23 - 5）点的检修记录
    private Map<String, DeviceBean> deviceMapping = new HashMap<String, DeviceBean>();//设备id -> 设备信息
    private Map<String, List<AlarmBean>> deviceErrorNum = new HashMap<String, List<AlarmBean>>();//设备ID -> 告警信息
    private List<List<EventBean>> doubtfulOverhulEvents = new ArrayList<List<EventBean>>();//疑似检修
    private Integer count = 0;//有效报警
    //临时记录
    private Map<String, Long> subwayErrorTime = new HashMap<String, Long>();//每个站最后一次报警时间
    private Map<String, List<EventBean>> subwayErrorEvent = new HashMap<String, List<EventBean>>();//
    private Set<Integer> doubtfulEventsId = new HashSet<Integer>();//疑似的ID
    //一个车站5分种内 报警 超过5次 判定为 疑似检修
    private static Long LIMIT_ERROR_TIME = 1000 * 60 * 10L;//最小报警间隔
    private static Integer LIMIT_ERROR_NUM = 5;//最小报警数

    public DataHandler(List<EventBean> events) {
        this.events = events;
    }

    public void print() {
        System.out.println("疑似检修次数："+doubtfulEventsId.size());
        System.out.println("有效报警数" + count);

        System.out.println("午夜检修告警："+overhaulEvents.size());
        System.out.println("---各站有效报警数---");
        for (Map.Entry<String, List<AlarmBean>> entry : deviceErrorNum.entrySet()) {
            System.out.println(deviceMapping.get(entry.getKey()).getName() + "  " + entry.getValue().size());
        }
        for(List<EventBean> eventBeans:doubtfulOverhulEvents){

            for(EventBean e : eventBeans){
                System.out.print(ExcelUtils.DATEFORMAT.format(new Date(e.getTimeStamp())) +" ");
                System.out.print(e.getDevice().getName() +" ");
                System.out.print(e.getDevice().getSubway()+" ");
                System.out.print(e.getDevice().getId()+" ");
                System.out.print(e.getAlarm().getInfo());
                System.out.println();
            }
            System.out.println("---------");
        }


    }

    public void handle() {
        Iterator<EventBean> it = events.iterator();
        //第一次先把疑似检修和确认的检修剔除，生成 设备基本信息
        while (it.hasNext()) {
            EventBean event = it.next();
            Date time = new Date(event.getTimeStamp());
            calendar.setTime(time);

            if (checkDeviceOverhaul(event)) {
                it.remove();
                continue;
            }

            checkDoubtfulOverhul(event);
            if (!deviceMapping.containsKey(event.getDevice().getId())) {
                deviceMapping.put(event.getDevice().getId(), event.getDevice());
            }
        }

        //再次检查是否有疑似检修
        for (Map.Entry<String, List<EventBean>> errorEntry : subwayErrorEvent.entrySet()) {
            if (errorEntry.getValue().size() > LIMIT_ERROR_NUM) {
                for (EventBean bean : errorEntry.getValue()) {
                    doubtfulEventsId.add(bean.getId());
                }
                doubtfulOverhulEvents.add(errorEntry.getValue());
            }
        }

        //处理可确信的报告
        for (EventBean event : events) {
            if (!doubtfulEventsId.contains(event.getId())) {//非疑似报警
                if (deviceErrorNum.containsKey(event.getDevice().getId())) {
                    deviceErrorNum.get(event.getDevice().getId()).add(event.getAlarm());
                } else {
                    List<AlarmBean> alarms = new ArrayList<AlarmBean>();
                    alarms.add(event.getAlarm());
                    deviceErrorNum.put(event.getDevice().getId(), alarms);
                }
                this.count++;
            }
        }
    }

    private void checkDoubtfulOverhul(EventBean eventBean) {
        if (StringUtils.isNotEmpty(eventBean.getDevice().getSubway())) {
            String subway = eventBean.getDevice().getSubway();
            if (subwayErrorTime.containsKey(subway)) {//该站出现过报警
                Long oldTime = subwayErrorTime.get(subway);
                if (eventBean.getTimeStamp() - oldTime < LIMIT_ERROR_TIME) {//小于5分钟
                    subwayErrorEvent.get(subway).add(eventBean);
                    subwayErrorTime.put(subway, eventBean.getTimeStamp());
                } else {//超过5分钟，判断上一次
                    if (subwayErrorEvent.get(subway).size() >= LIMIT_ERROR_NUM) {//是疑似检修
                        for (EventBean bean : subwayErrorEvent.get(subway)) {
                            doubtfulEventsId.add(bean.getId());
                        }
                        doubtfulOverhulEvents.add(subwayErrorEvent.get(subway));
                    }
                    subwayErrorTime.remove(subway);
                    subwayErrorEvent.remove(subway);
                    List<EventBean> newEventBean = new ArrayList<EventBean>();
                    newEventBean.add(eventBean);
                    subwayErrorEvent.put(subway, newEventBean);
                    subwayErrorTime.put(subway, eventBean.getTimeStamp());
                }
            } else {//该站未出现过报警
                List<EventBean> newEventBean = new ArrayList<EventBean>();
                newEventBean.add(eventBean);
                subwayErrorEvent.put(subway, newEventBean);
                subwayErrorTime.put(subway, eventBean.getTimeStamp());
            }
        }
    }


    //0-4 23以后 设备检修
    private Boolean checkDeviceOverhaul(EventBean eventBean) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 5 || hour >= 23) {
            overhaulEvents.add(eventBean);
            return true;
        }
        return false;
    }
}
