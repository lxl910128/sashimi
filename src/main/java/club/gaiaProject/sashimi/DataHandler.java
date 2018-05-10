package club.gaiaProject.sashimi;

import com.qihoo.wzws.rzb.secure.AnalyzeSingle;

import club.gaiaProject.sashimi.util.ExcelUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.util.*;

import club.gaiaProject.sashimi.bean.AlarmBean;
import club.gaiaProject.sashimi.bean.DeviceBean;
import club.gaiaProject.sashimi.bean.EventBean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class DataHandler {
    private List<EventBean> events;//execl 中的全部记录
    private Long startTime;
    private Long endTime;
    private Integer dayCount;
    //采集信息
    private List<EventBean> overhaulEvents = new ArrayList<EventBean>();//（23 - 5）点的检修记录
    private Calendar calendar = Calendar.getInstance();
    private Map<String, DeviceBean> deviceMapping = new HashMap<String, DeviceBean>();//设备id -> 设备信息
    private Map<String, List<AlarmBean>> deviceErrorNum = new HashMap<>();//设备ID -> 告警信息
    private List<List<EventBean>> doubtfulOverhulEvents = new ArrayList<List<EventBean>>();//疑似检修
    private Integer count = 0;//有效报警
    //临时记录
    private Map<String, Long> subwayErrorTime = new HashMap<String, Long>();//每个站最后一次报警时间
    private Map<String, List<EventBean>> subwayErrorEvent = new HashMap<String, List<EventBean>>();//
    private Set<Integer> doubtfulEventsId = new HashSet<Integer>();//疑似的ID
    //一个车站10分种内 报警 超过5次 判定为 疑似检修
    private static Long LIMIT_ERROR_TIME = 1000 * 60 * 10L;//最小报警间隔
    private Integer LIMIT_ERROR_NUM;//最小报警数

    public DataHandler(List<EventBean> events) {
        this.events = events;
    }

    public void createHTML() {
        Properties properties = new Properties();
        properties.setProperty("ISO-8859-1", "UTF-8");
        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");
        if (AnalyzeSingle.isJarExecute) {
            properties.setProperty("resource.loader", "jar");
            properties.setProperty("jar.resource.loader.class", "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
            properties.setProperty("jar.resource.loader.path", "jar:file:" + AnalyzeSingle.jarPath);
        } else {
            properties.setProperty("resource.loader", "class");
            properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        }

        VelocityEngine velocityEngine = new VelocityEngine(properties);
        Template template = velocityEngine.getTemplate("club/gaiaProject/sashimi/template/security-data.html", "UTF-8");
        VelocityContext context = new VelocityContext();
        //构造展示数据
        String fromDate = ExcelUtils.dateFormat.format(new Date(startTime)) + "至" + ExcelUtils.dateFormat.format(new Date(endTime));

        context.put("datetime", ExcelUtils.dateTimeFormat.format(new Date()));//头部时间
        context.put("fromDate", fromDate);//数据时间范围

    }

    public void print() {
        System.out.println("数据开始时间：" + ExcelUtils.dateTimeFormat.format(new Date(startTime)));
        System.out.println("数据结束时间：" + ExcelUtils.dateTimeFormat.format(new Date(endTime)));
        System.out.println("数据跨度：" + dayCount + "天");
        System.out.println("疑似检修次数：" + doubtfulEventsId.size());
        System.out.println("有效报警数" + count);

        System.out.println("午夜检修告警：" + overhaulEvents.size());
        System.out.println("---有效报警数---");
        for (Map.Entry<String, List<AlarmBean>> entry : deviceErrorNum.entrySet()) {
            System.out.println(deviceMapping.get(entry.getKey()).getSubway() + "  " + deviceMapping.get(entry.getKey()).getName() + "  " + entry.getValue().size());
        }
        System.out.println("————疑似检修数据————");
        for (List<EventBean> eventBeans : doubtfulOverhulEvents) {

            for (EventBean e : eventBeans) {
                System.out.print(ExcelUtils.dateTimeFormat.format(new Date(e.getTimeStamp())) + " ");
                System.out.print(e.getDevice().getName() + " ");
                System.out.print(e.getDevice().getSubway() + " ");
                System.out.print(e.getDevice().getId() + " ");
                System.out.print(e.getAlarm().getInfo());
                System.out.println();
            }
            System.out.println("---------");
        }
        System.out.println("————作业产生告警数据————");
        for (EventBean e : overhaulEvents) {

            System.out.print(ExcelUtils.dateTimeFormat.format(new Date(e.getTimeStamp())) + " ");
            System.out.print(e.getDevice().getName() + " ");
            System.out.print(e.getDevice().getSubway() + " ");
            System.out.print(e.getDevice().getId() + " ");
            System.out.print(e.getAlarm().getInfo());
            System.out.println();
        }


    }

    public void handle() {
        EventBean startEventBean = events.get(0);
        EventBean endEventBean = events.get(events.size() - 1);
        startTime = startEventBean.getTimeStamp();
        endTime = endEventBean.getTimeStamp();
        Long dayStamp = (endTime - startTime) / (1000 * 60 * 60 * 24L);
        dayCount = dayStamp.intValue() + 1;//天数
        if (dayCount >= 30) {
            LIMIT_ERROR_NUM = 5;
            System.out.println("阈值：" + 5);
        } else {
            LIMIT_ERROR_NUM = 3;
            System.out.println("阈值：" + 3);
        }
        Iterator<EventBean> it = events.iterator();
        //第一次先把疑似检修和确认的检修剔除，生成 设备基本信息
        while (it.hasNext()) {
            EventBean event = it.next();
            Date time = new Date(event.getTimeStamp());
            calendar.setTime(time);

            //验证此条告警是否是 午夜检修
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
                //


                //各个设备有多少次
                if (deviceErrorNum.containsKey(event.getDevice().getId())) {
                    deviceErrorNum.get(event.getDevice().getId()).add(event.getAlarm());
                } else {
                    List<AlarmBean> alarms = new ArrayList<AlarmBean>();
                    alarms.add(event.getAlarm());
                    deviceErrorNum.put(event.getDevice().getId(), alarms);
                }

                //有效告警的总数
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


    //0-4 23以后 设备检修 判定为 午夜检修
    private Boolean checkDeviceOverhaul(EventBean eventBean) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 4 || hour >= 23) {
            overhaulEvents.add(eventBean);
            return true;
        }
        return false;
    }
}
