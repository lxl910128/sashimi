package club.gaiaProject.sashimi;

import club.gaiaProject.sashimi.bean.*;
import club.gaiaProject.sashimi.util.ExcelUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Map<String, List<EventBean>> deviceErrorNum = new HashMap<>();//设备ID -> 告警信息
    private Map<String, Integer> deviceTypeErrorNum = new TreeMap<>();//设备Type -> count
    private List<List<EventBean>> doubtfulOverhulEvents = new ArrayList<List<EventBean>>();//疑似检修
    private Integer alarmCount = 0;//有效报警
    private Integer eventCount = 0; //日志总数
    private Map<String, Integer> stationAlarm = new HashMap<>();//各站报警数
    private Map<String, Integer> alarm = new HashMap<>();//各级别报警数
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
    private Integer limitDeviceFoucs = 4;//某类设备有效告警超过此值即为 重点关注对象

    private Map<String, List<FixEventBean>> fixEvent;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private List<ExcelBean> excelData = new ArrayList<>();

    private static Set<String> zhengxian1 = new HashSet<>();
    private static Set<String> zhengxian2 = new HashSet<>();
    private static Set<String> zhengxian3 = new HashSet<>();

    public DataHandler() {
    }

    public List<List<EventBean>> getDoubtfulOverhulEvents() {
        return doubtfulOverhulEvents;
    }

    public List<ExcelBean> getExcelOut() {
        return this.excelData;
    }


    public DataHandler(List<EventBean> events, Map<String, List<FixEventBean>> fixEvent) {
        this.events = events;
        this.fixEvent = fixEvent;

        zhengxian1.add("刘庄站");
        zhengxian1.add("柳林站");
        zhengxian1.add("沙门站");
        zhengxian1.add("北三环站");
        zhengxian1.add("东风路站");
        zhengxian1.add("关虎屯站");
        zhengxian1.add("黄河路站");
        zhengxian1.add("紫荆山站");

        zhengxian2.add("东大街站");
        zhengxian2.add("陇海东路站");
        zhengxian2.add("二里岗站");
        zhengxian2.add("南五里堡站");
        zhengxian2.add("花寨站");
        zhengxian2.add("南三环站");
        zhengxian2.add("站马屯站");
        zhengxian2.add("南四环站");

        zhengxian3.add("车辆段");
    }

    public void outputExcel(File file) throws Exception {
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();

    }

    public String createHTML(File file) throws Exception {
        VelocityEngine velocityEngine = new VelocityEngine();

        Properties properties = new Properties();
        properties.setProperty(Velocity.ENCODING_DEFAULT, "UTF-8");
        properties.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
        properties.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8");
        properties.setProperty("resource.loader", "file");
        properties.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        velocityEngine.init(properties);

        Template template = velocityEngine.getTemplate("security-data.html", "UTF-8");
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

        Map<String, Integer> bing = new HashedMap();
        for (Map.Entry<String, List<EventBean>> entry : deviceErrorNum.entrySet()) {
            String subway = entry.getValue().get(0).getDevice().getSubway();
            if (zhengxian1.contains(subway)) {
                if (bing.containsKey("正线一范围")) {
                    bing.put("正线一范围", bing.get("正线一范围") + entry.getValue().size());
                } else {
                    bing.put("正线一范围", entry.getValue().size());
                }
            }
            if (zhengxian2.contains(subway)) {
                if (bing.containsKey("正线二范围")) {
                    bing.put("正线二范围", bing.get("正线二范围") + entry.getValue().size());
                } else {
                    bing.put("正线二范围", entry.getValue().size());
                }
            }
            if (zhengxian3.contains(subway)) {
                if (bing.containsKey("车辆段")) {
                    bing.put("车辆段", bing.get("车辆段") + entry.getValue().size());
                } else {
                    bing.put("车辆段", entry.getValue().size());
                }
            }
        }

        bing.forEach((x, y) -> {
            JSONObject obj = new JSONObject();
            obj.put("name", x);
            obj.put("value", y);
            typeList.add(obj);
        });

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
            node.put("text", "others");
            node.put("max", maxValue);
            typeKeyList.add(4, node);
            typeValue.add(4, count);
        }
        JSONObject radarData = new JSONObject();
        radarData.put("keyList", typeKeyList);
        radarData.put("valueList", typeValue);
        context.put("radarData", radarData.toString());

        //高危设备
        List<OutputBean> effective = new ArrayList<>();
        deviceErrorNum.forEach((x, y) -> {
            if (y.size() >= limitFoucs) {
                DeviceBean deviceBean = deviceMapping.get(x);
                OutputBean output = new OutputBean();
                output.setName(deviceBean.getName());
                List<EventVO> data = new ArrayList<>();
                y.forEach(z -> {
                    data.add(z.toEventVO());
                });
                output.setEventList(data);
                output.setCount(y.size() + "");
                effective.add(output);
            }
        });
        effective.sort((x, y) -> {
            return y.getEventList().size() - x.getEventList().size();
        });
        context.put("effectiveList", effective);

        //疑似检修
        List<OutputBean> doubtfulData = new ArrayList<>();
        for (List<EventBean> events : doubtfulOverhulEvents) {
            OutputBean doubtful = new OutputBean();
            List<EventVO> doubtfulList = new ArrayList<>();
            Long max = null;
            Long min = null;
            for (EventBean event : events) {
                if (doubtful.getSubway() == null) {
                    doubtful.setSubway(event.getDevice().getSubway());
                }
                if (max == null || event.getTimeStamp() > max) {
                    max = event.getTimeStamp();
                }
                if (min == null || event.getTimeStamp() < min) {
                    min = event.getTimeStamp();
                }
                doubtfulList.add(event.toEventVO());
            }
            if (max != null) {
                doubtful.setEndTime(ExcelUtils.dateTimeFormat.format(new Date(max)));
            } else {
                doubtful.setEndTime("");
            }
            if (min != null) {
                doubtful.setStartTime(ExcelUtils.dateTimeFormat.format(new Date(min)));
            } else {
                doubtful.setStartTime("");
            }
            doubtful.setCount(doubtfulList.size() + "");
            doubtfulList.sort((x, y) -> {
                if (x.getTime().equals(y.getTime())) {
                    return x.getName().compareTo(y.getName());
                } else {
                    return x.getTime().compareTo(y.getTime());
                }
            });
            doubtful.setEventList(doubtfulList);
            doubtfulData.add(doubtful);
        }
        context.put("doubtfulList", doubtfulData);

        //生命线
        context.put("evt_subwaySum", stationAlarmList.size());
        context.put("max_subway", stationAlarmList.get(0).getName());
        context.put("max_subwayCount", stationAlarmList.get(0).getValue());
        context.put("limitFoucs", limitFoucs + "");
        context.put("bad_device", effective.size() + "");
        List<Map.Entry<String, Integer>> alarmList = new ArrayList<>(alarm.entrySet());
        alarmList.sort((x, y) -> {
            return y.getValue().compareTo(x.getValue());
        });
        context.put("max_alarmType", alarmList.get(0).getKey());
        context.put("max_alarmConut", alarmList.get(0).getValue() + 1);
        List<Map.Entry<String, Integer>> deviceTypeList = new ArrayList<>(deviceTypeErrorNum.entrySet());
        deviceTypeList.sort((x, y) -> {
            return y.getValue().compareTo(x.getValue());
        });
        context.put("max_deviceType", deviceTypeList.get(0).getKey());
        context.put("max_deviceConut", deviceTypeList.get(0).getValue() + 1);

        //高危设备统计
        List<DeviceBean> gaoweiCount = new ArrayList<>();
        deviceErrorNum.forEach((x, y) -> {
            Integer count = y.size();
            if (count >= limitFoucs) {
                DeviceBean d = y.get(0).getDevice();
                d.setCount(count);
                gaoweiCount.add(d);
            }
        });
        gaoweiCount.sort((x, y) -> {
            return y.getCount() - x.getCount();
        });
        context.put("gaoweiCount", gaoweiCount);
        if (!file.exists()) {
            file.createNewFile();
        }


        //新表
        List<AnalysisBean> fistTable = new ArrayList<>();
        deviceErrorNum.forEach((x, y) -> {
            DeviceBean device = y.get(0).getDevice();
            AnalysisBean analysisBean = new AnalysisBean();
            analysisBean.setSubway(device.getSubway());
            analysisBean.setType(device.getUserDefinedType());
            analysisBean.setDeviceName(device.getName());
            // 摄录设备 超过阈值
            if (checkDeviceType(device.getName()) && y.size() >= limitFoucs) {
                Float fenshu = ((float) y.size()) / limitFoucs;
                analysisBean.setAnalysisInfo(String.format("告警次数%d,超出告警阈值%d", y.size(), (Math.round(fenshu * 100))) + "%");
                analysisBean.setHandlerInfo("排查该摄像机各个节点是否存在接头接触不良/损坏等现象");
                fistTable.add(analysisBean);

                ExcelBean excel = new ExcelBean();
                excel.setSubway(analysisBean.getSubway());
                excel.setDeviceName(device.getName());
                excel.setAlarmCount(y.size() + "");
                for (EventBean e : y) {
                    if (excel.getLastTime() == null || excel.getLastTime() < e.getTimeStamp()) {
                        excel.setLastTime(e.getTimeStamp());
                    }
                    excel.getAlarmInfo().add(e.getAlarm().getInfo());
                }
                excelData.add(excel);
            }
            // 视频传输设备
            if (!checkDeviceType(device.getName())) {
                analysisBean.setAnalysisInfo("设备告警,次数" + y.size());
                analysisBean.setHandlerInfo("关注设备接电/工作状态");
                fistTable.add(analysisBean);

                ExcelBean excel = new ExcelBean();
                excel.setSubway(analysisBean.getSubway());
                excel.setDeviceName(device.getName());
                excel.setAlarmCount(y.size() + "");
                for (EventBean e : y) {
                    if (excel.getLastTime() == null || excel.getLastTime() < e.getTimeStamp()) {
                        excel.setLastTime(e.getTimeStamp());
                    }
                    excel.getAlarmInfo().add(e.getAlarm().getInfo());
                }
                excelData.add(excel);
            }


        });
        // 疑似数据添加新表
        doubtfulOverhulEvents.forEach((x) -> {
            List<AnalysisBean> get = getAnalysis(x);
            fistTable.addAll(get);
        });
        context.put("fistTable", fistTable);

        File out = new File(file.getCanonicalPath()+File.separator+fromDate+"数据报告.html");
        out.createNewFile();
        PrintWriter pw = new PrintWriter(out, "UTF-8");
        template.merge(context, pw);
        pw.close();

        return fromDate;

    }

    private List<AnalysisBean> getAnalysis(List<EventBean> input) {
        List<AnalysisBean> ret = new ArrayList<>();
        Map<Long, List<EventBean>> tmp = new HashMap<>();
        for (EventBean event : input) {
            event.setTmpName(getNumInDevName(event.getDevice().getName()));
            if (tmp.containsKey(event.getTimeStamp())) {
                tmp.get(event.getTimeStamp()).add(event);
            } else {
                List<EventBean> newList = new ArrayList<>();
                newList.add(event);
                tmp.put(event.getTimeStamp(), newList);
            }
        }

        tmp.forEach((x, y) -> {
            y.sort((o1, o2) -> {
                Integer r = o1.getTmpName() - o2.getTmpName();
                return r;
            });
        });

        for (List<EventBean> list : tmp.values()) {
            int flag = 0;
            Integer tmpName = null;
            List<EventBean> tmpList = new ArrayList<>();
            StringBuffer fusionName = new StringBuffer();
            for (EventBean eventBean : list) {
                if (eventBean.getTmpName() == -1) {
                    continue;
                }
                if (tmpName == null) {
                    fusionName.append(eventBean.getDevice().getName()).append("; ");
                    tmpName = eventBean.getTmpName();
                    tmpList.add(eventBean);
                    flag++;
                    continue;
                }
                Integer aa = tmpName - eventBean.getTmpName();
                if (aa == 1 || aa == -1) {
                    fusionName.append(eventBean.getDevice().getName()).append("; ");
                    tmpName = eventBean.getTmpName();
                    tmpList.add(eventBean);
                    flag++;
                    continue;
                } else {
                    if (flag >= 4) {
                        DeviceBean device = eventBean.getDevice();
                        AnalysisBean analysisBean = new AnalysisBean();
                        analysisBean.setSubway(device.getSubway());
                        analysisBean.setType("视频传输设备");
                        analysisBean.setAnalysisInfo("设备：" + fusionName.substring(0, fusionName.length() - 1) + timeFormat.format(new Date(eventBean.getTimeStamp())) + "同时告警");
                        analysisBean.setHandlerInfo("建议检查上游设备，排查隔离地单元/字符串叠加分配器设备接电/工作状态");
                        analysisBean.setDeviceName(" ");
                        ret.add(analysisBean);


                        ExcelBean excel = new ExcelBean();
                        excel.setSubway(device.getSubway());
                        excel.setDeviceName("视频传输设备");
                        excel.getAlarmInfo().add("设备：" + fusionName.toString() + timeFormat.format(new Date(eventBean.getTimeStamp())) + "同时告警");
                        excel.setAlarmCount(flag + "");
                        excel.setLastTime(eventBean.getTimeStamp());
                        this.excelData.add(excel);
                    }
                    flag = 1;
                    tmpName = eventBean.getTmpName();
                    tmpList.clear();
                    fusionName.delete(0, fusionName.length() - 1);
                    fusionName.append(eventBean.getDevice().getName()).append(";");
                    tmpList.add(eventBean);
                }
            }
            if (flag >= 4) {
                DeviceBean device = list.get(0).getDevice();
                AnalysisBean analysisBean = new AnalysisBean();
                analysisBean.setSubway(device.getSubway());
                analysisBean.setType("视频传输设备");
                analysisBean.setAnalysisInfo("设备：" + fusionName.toString() + timeFormat.format(new Date(list.get(0).getTimeStamp())) + "同时告警");
                analysisBean.setHandlerInfo("建议检查上游设备，排查隔离地单元/字符串叠加分配器设备接电/工作状态");
                analysisBean.setDeviceName(" ");
                ret.add(analysisBean);

                ExcelBean excel = new ExcelBean();
                excel.setSubway(device.getSubway());
                excel.setDeviceName("视频传输设备");
                excel.getAlarmInfo().add("设备：" + fusionName.toString() + timeFormat.format(new Date(list.get(0).getTimeStamp())) + "同时告警");
                excel.setAlarmCount(flag + "");
                excel.setLastTime(list.get(0).getTimeStamp());
                this.excelData.add(excel);
            }
        }

        return ret;
    }


    private Integer getNumInDevName(String name) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(name);
        String newName = m.replaceAll("").trim();
        if (StringUtils.isNotEmpty(newName)) {
            return Integer.valueOf(m.replaceAll("").trim());
        } else {
            return -1;
        }
    }

    public void print() {
        System.out.println("数据开始时间：" + ExcelUtils.dateTimeFormat.format(new Date(startTime)));
        System.out.println("数据结束时间：" + ExcelUtils.dateTimeFormat.format(new Date(endTime)));
        System.out.println("数据跨度：" + dayCount + "天");
        System.out.println("疑似检修次数：" + doubtfulEventsId.size());
        System.out.println("有效报警数" + alarmCount);

        System.out.println("午夜检修告警：" + overhaulEvents.size());
        System.out.println("---有效报警数---");
        for (Map.Entry<String, List<EventBean>> entry : deviceErrorNum.entrySet()) {
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
            //设备类型
            if (checkDeviceType(event.getDevice().getName())) {
                event.getDevice().setUserDefinedType("视频摄录设备");
            } else {
                event.getDevice().setUserDefinedType("视频传输设备");
            }
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
                    deviceErrorNum.get(event.getDevice().getId()).add(event);
                } else {
                    List<EventBean> alarms = new ArrayList<EventBean>();
                    alarms.add(event);
                    deviceErrorNum.put(event.getDevice().getId(), alarms);
                }
                //告警类型分析
                if (alarm.containsKey(event.getAlarm().getLevel())) {
                    alarm.put(event.getAlarm().getLevel(), alarm.get(event.getAlarm().getLevel()) + 1);
                } else {
                    alarm.put(event.getAlarm().getLevel(), 1);
                }

                //有效告警的总数
                this.alarmCount++;
            } else {
                //折线图 疑似告警+1
                lineMap.get(key)[3] = lineMap.get(key)[3] + 1;
            }
        }
    }

    private boolean checkDeviceType(String deviceName) {
        if (deviceName.startsWith("ZM")) {
            return true;
        } else {
            return false;
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
    /*private Boolean checkDeviceOverhaul(EventBean eventBean) {

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 4 || hour >= 23) {
            overhaulEvents.add(eventBean);
            return true;
        }
        return false;
    }*/

    private Boolean checkDeviceOverhaul(EventBean eventBean) {
        if (this.fixEvent.containsKey(eventBean.getDevice().getSubway())) {
            List<FixEventBean> fixList = this.fixEvent.get(eventBean.getDevice().getSubway());
            for (FixEventBean fix : fixList) {
                if (fix.getStartTime() <= eventBean.getTimeStamp() && eventBean.getTimeStamp() <= fix.getEndTime()) {
                    overhaulEvents.add(eventBean);
                    return true;
                }
            }
        }
        return false;
    }

    private String getDateKey(Calendar s) {
        return dateFormat.format(s.getTime());
    }


}


