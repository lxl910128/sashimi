package club.gaiaProject.sashimi;

import club.gaiaProject.sashimi.bean.CountBean;
import club.gaiaProject.sashimi.util.ExcelUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.PrintWriter;
import java.util.*;

import club.gaiaProject.sashimi.bean.AlarmBean;
import club.gaiaProject.sashimi.bean.DeviceBean;
import club.gaiaProject.sashimi.bean.EventBean;
import org.apache.velocity.app.VelocityEngine;


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
    private Map<String, Integer> deviceTypeErrorNum = new TreeMap<>();//设备Type -> count
    private List<List<EventBean>> doubtfulOverhulEvents = new ArrayList<List<EventBean>>();//疑似检修
    private Integer alarmCount = 0;//有效报警
    private Integer eventCount = 0; //日志总数
    private Map<String, Integer> stationAlarm = new HashMap<>();//各站报警数
    //0 总数， 1 有效告警 2 午夜检修 3 疑似检修
    private Map<String, int[]> lineMap = new TreeMap<>();//构造折线图数据
    //临时记录
    private Map<String, Long> subwayErrorTime = new HashMap<String, Long>();//每个站最后一次报警时间
    private Map<String, List<EventBean>> subwayErrorEvent = new HashMap<String, List<EventBean>>();//
    private Set<Integer> doubtfulEventsId = new HashSet<Integer>();//疑似的ID
    //一个车站10分种内 报警 超过5次 判定为 疑似检修
    private static Long LIMIT_ERROR_TIME = 1000 * 60 * 10L;//最小报警间隔
    private static Integer LIMIT_ERROR_NUM = 5;//最小报警数

    private Integer limitFoucs = 0;//某类设备有效告警超过此值即为 重点关注对象

    public DataHandler(List<EventBean> events) {
        this.events = events;
    }

    public void createHTML() throws Exception {
        VelocityEngine velocityEngine = new VelocityEngine();
        Properties properties = new Properties();
        properties.setProperty("ISO-8859-1", "UTF-8");
        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");
        velocityEngine.init(properties);

        Template template = velocityEngine.getTemplate("security-data.vm", "UTF-8");
        VelocityContext context = new VelocityContext();
        //构造展示数据
        String fromDate = ExcelUtils.dateFormat.format(new Date(startTime)) + "--" + ExcelUtils.dateFormat.format(new Date(endTime));

        context.put("datetime", ExcelUtils.dateTimeFormat.format(new Date()));//头部时间
        context.put("fromDate", fromDate);//数据时间范围
        context.put("eventCount", eventCount);//日志总数
        context.put("alarmCount", alarmCount);//有效告警数
        context.put("overhaulCount", overhaulEvents.size());//午夜检修数
        context.put("doubtfulCount", doubtfulEventsId.size());//疑似检修数
        context.put("doubtfulThreshold", LIMIT_ERROR_NUM);//疑似检修的阈值
        context.put("doubtfulTime", LIMIT_ERROR_TIME / 1000 / 60);//疑似检修的时间阈值
        context.put("dateSum", dayCount);
        List<CountBean> stationAlarmList = new ArrayList<>();
        stationAlarm.forEach((x, y) -> {
            stationAlarmList.add(new CountBean(x, y.toString()));
        });
        stationAlarmList.sort((x, y) -> {
            Integer xInt = Integer.valueOf(x.getValue());
            Integer yInt = Integer.valueOf(y.getValue());
            return yInt - xInt;
        });
        context.put("stationAlarmList", stationAlarmList);//各站点告警数
        //构造折线图数据
        List<String> xAxis = new ArrayList<>();//直线图X轴
        List<String> y1 = new ArrayList<>();//告警总数
        List<String> y2 = new ArrayList<>();//有效告警
        List<String> y3 = new ArrayList<>();//午夜检修
        List<String> y4 = new ArrayList<>();//疑似检修

        lineMap.forEach((x, y) -> {
            xAxis.add(x);
            y1.add(y[0] + "");
            y2.add(y[1] + "");
            y3.add(y[2] + "");
            y4.add(y[3] + "");
        });

        Map<String, List<String>> lineData = new HashMap();//用于绘制折线图
        lineData.put("xData", xAxis);
        lineData.put("all", y1);
        lineData.put("effective", y2);
        lineData.put("overhaul", y3);
        lineData.put("doubtful", y4);
        context.put("lineData", JSON.toJSON(lineData).toString());
        //构造饼图数据
        JSONObject bingData = new JSONObject();
        JSONArray typeList = new JSONArray();
        Integer otherSize = 0;
        for (Map.Entry<String, List<AlarmBean>> entry : deviceErrorNum.entrySet()) {
            if (entry.getValue().size() >= limitFoucs) {
                DeviceBean deviceBean = deviceMapping.get(entry.getKey());
                JSONObject type = new JSONObject();
                type.put("name", deviceBean.getName());
                type.put("value", entry.getValue().size());
                typeList.add(type);
            } else {
                otherSize += entry.getValue().size();
            }
        }
        if (otherSize > 0) {
            JSONObject type = new JSONObject();
            type.put("name", new String("其它"));
            type.put("value", otherSize);
            typeList.add(type);
        }

        bingData.put("typeList", typeList);
        context.put("bingData", bingData.toString());

        //构造雷达图
        List<Map<String, Object>> typeKeyList = new ArrayList<>();
        List<Integer> typeValue = new ArrayList<>();
        List<Map.Entry<String, Integer>> listSort = new ArrayList<>(deviceTypeErrorNum.entrySet());
        listSort.sort((x, y) -> {
            return y.getValue().compareTo(x.getValue());
        });
        Integer maxValue = 0;
        for (int i = 0; i < 5; i++) {
            if (i < listSort.size()) {
                Map.Entry<String, Integer> type = listSort.get(i);

                if (maxValue == 0) {
                    maxValue = type.getValue();
                }

                Map<String, Object> node = new HashedMap();
                node.put("text", type.getKey());
                node.put("max", maxValue);
                typeKeyList.add(node);
                typeValue.add(type.getValue());
            } else {
                Map<String, Object> node = new HashedMap();
                node.put("text", "");
                node.put("max", maxValue);
                typeKeyList.add(node);
                typeValue.add(0);
            }
        }
        if (listSort.size() > 5) {

            int count = 0;
            for (int i = 4; i < listSort.size(); i++) {
                count += listSort.get(i).getValue();
            }
            Map<String, Object> node = new HashedMap();
            node.put("text", "其他");
            node.put("max", maxValue);
            typeKeyList.add(4, node);
            typeValue.add(4, count);
        }
        JSONObject radarData = new JSONObject();
        radarData.put("keyList", typeKeyList);
        radarData.put("valueList", typeValue);
        context.put("radarData", radarData.toString());


        PrintWriter pw = new PrintWriter("C:\\Users\\Administrator\\Desktop\\out.html", "UTF-8");
        template.merge(context, pw);
        pw.close();


    }

    public void print() {
        System.out.println("数据开始时间：" + ExcelUtils.dateTimeFormat.format(new Date(startTime)));
        System.out.println("数据结束时间：" + ExcelUtils.dateTimeFormat.format(new Date(endTime)));
        System.out.println("数据跨度：" + dayCount + "天");
        System.out.println("疑似检修次数：" + doubtfulEventsId.size());
        System.out.println("有效报警数" + alarmCount);

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
        //计算起止时间
        EventBean startEventBean = events.get(0);
        EventBean endEventBean = events.get(events.size() - 1);
        startTime = startEventBean.getTimeStamp();
        endTime = endEventBean.getTimeStamp();
        initLineData(startTime, endTime);
        Long dayStamp = (endTime - startTime) / (1000 * 60 * 60 * 24L);
        eventCount = events.size();
        dayCount = dayStamp.intValue() + 1;//天数
        if (dayCount >= 30) {
            limitFoucs = 5;
            System.out.println("阈值：" + 5);
        } else {
            limitFoucs = 3;
            System.out.println("阈值：" + 3);
        }
        Iterator<EventBean> it = events.iterator();
        //第一次先把疑似检修和确认的检修剔除，生成 设备基本信息
        while (it.hasNext()) {
            EventBean event = it.next();
            calendar.setTimeInMillis(event.getTimeStamp());
            String key = getDateKey(calendar);
            //折线图 总高告警数累加1
            lineMap.get(key)[0] = lineMap.get(key)[0] + 1;

            //验证此条告警是否是 午夜检修
            if (checkDeviceOverhaul(event)) {
                //折线图 午夜检修累加1
                lineMap.get(key)[2] = lineMap.get(key)[2] + 1;
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
            calendar.setTimeInMillis(event.getTimeStamp());
            String key = getDateKey(calendar);

            if (!doubtfulEventsId.contains(event.getId())) {//非疑似报警
                lineMap.get(key)[1] = lineMap.get(key)[1] + 1;

                //各站报警数
                if (stationAlarm.containsKey(event.getDevice().getSubway())) {
                    stationAlarm.put(event.getDevice().getSubway(), stationAlarm.get(event.getDevice().getSubway()) + 1);
                } else {
                    stationAlarm.put(event.getDevice().getSubway(), 1);
                }

                //各设备类型多少次
                String typeName = event.getDevice().getTypeName();
                if (StringUtils.isEmpty(typeName)) {
                    typeName = "未知";
                }
                if (deviceTypeErrorNum.containsKey(typeName)) {
                    deviceTypeErrorNum.put(typeName, deviceTypeErrorNum.get(typeName) + 1);
                } else {
                    deviceTypeErrorNum.put(typeName, 1);
                }


                //各个设备有多少次
                if (deviceErrorNum.containsKey(event.getDevice().getId())) {
                    deviceErrorNum.get(event.getDevice().getId()).add(event.getAlarm());
                } else {
                    List<AlarmBean> alarms = new ArrayList<AlarmBean>();
                    alarms.add(event.getAlarm());
                    deviceErrorNum.put(event.getDevice().getId(), alarms);
                }

                //有效告警的总数
                this.alarmCount++;
            } else {
                //折线图 疑似告警+1
                lineMap.get(key)[3] = lineMap.get(key)[3] + 1;
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

    private void initLineData(Long start, Long end) {
        Calendar s = Calendar.getInstance();
        s.setTimeInMillis(start);
        s.set(Calendar.HOUR_OF_DAY, 0);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);

        Calendar e = Calendar.getInstance();
        e.setTimeInMillis(end);

        while (s.compareTo(e) <= 0) {
            String key = getDateKey(s);
            lineMap.put(key, new int[4]);
            s.add(Calendar.DAY_OF_MONTH, 1);
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

    private String getDateKey(Calendar s) {
        Integer day = s.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            return String.format("%s-%s-0%s", s.get(Calendar.YEAR), s.get(Calendar.MONTH), s.get(Calendar.DAY_OF_MONTH));
        } else {
            return String.format("%s-%s-%s", s.get(Calendar.YEAR), s.get(Calendar.MONTH), s.get(Calendar.DAY_OF_MONTH));
        }

    }

    public static void main(String[] args) {
    }

}


