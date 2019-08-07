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
    private List<EventBean> events;//execl �е�ȫ����¼
    private Long startTime;
    private Long endTime;
    private Integer dayCount;
    //�ɼ���Ϣ
    private List<EventBean> overhaulEvents = new ArrayList<EventBean>();//��23 - 5����ļ��޼�¼
    private Calendar calendar = Calendar.getInstance();
    private Map<String, DeviceBean> deviceMapping = new HashMap<String, DeviceBean>();//�豸id -> �豸��Ϣ
    private Map<String, List<EventBean>> deviceErrorNum = new HashMap<>();//�豸ID -> �澯��Ϣ
    private Map<String, Integer> deviceTypeErrorNum = new TreeMap<>();//�豸Type -> count
    private List<List<EventBean>> doubtfulOverhulEvents = new ArrayList<List<EventBean>>();//���Ƽ���
    private Integer alarmCount = 0;//��Ч����
    private Integer eventCount = 0; //��־����
    private Map<String, Integer> stationAlarm = new HashMap<>();//��վ������
    private Map<String, Integer> alarm = new HashMap<>();//�����𱨾���
    //0 ������ 1 ��Ч�澯 2 ��ҹ���� 3 ���Ƽ���
    private Map<String, int[]> lineMap = new TreeMap<>();//��������ͼ����
    //��ʱ��¼
    private Map<String, Long> subwayErrorTime = new HashMap<String, Long>();//ÿ��վ���һ�α���ʱ��
    private Map<String, List<EventBean>> subwayErrorEvent = new HashMap<String, List<EventBean>>();//
    private Set<Integer> doubtfulEventsId = new HashSet<Integer>();//���Ƶ�ID
    //һ����վ10������ ���� ����5�� �ж�Ϊ ���Ƽ���
    private static Long LIMIT_ERROR_TIME = 1000 * 60 * 10L;//��С�������
    private static Integer LIMIT_ERROR_NUM = 5;//��С������

    private Integer limitFoucs = 0;//ĳ���豸��Ч�澯������ֵ��Ϊ �ص��ע����
    private Integer limitDeviceFoucs = 4;//ĳ���豸��Ч�澯������ֵ��Ϊ �ص��ע����

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

        zhengxian1.add("��ׯվ");
        zhengxian1.add("����վ");
        zhengxian1.add("ɳ��վ");
        zhengxian1.add("������վ");
        zhengxian1.add("����·վ");
        zhengxian1.add("�ػ���վ");
        zhengxian1.add("�ƺ�·վ");
        zhengxian1.add("�Ͼ�ɽվ");

        zhengxian2.add("�����վ");
        zhengxian2.add("¤����·վ");
        zhengxian2.add("�����վ");
        zhengxian2.add("�����ﱤվ");
        zhengxian2.add("��կվ");
        zhengxian2.add("������վ");
        zhengxian2.add("վ����վ");
        zhengxian2.add("���Ļ�վ");

        zhengxian3.add("������");
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
        //����չʾ����
        String fromDate = ExcelUtils.dateFormat.format(new Date(startTime)) + "--" + ExcelUtils.dateFormat.format(new Date(endTime));

        context.put("datetime", ExcelUtils.dateTimeFormat.format(new Date()));//ͷ��ʱ��
        context.put("fromDate", fromDate);//����ʱ�䷶Χ
        context.put("eventCount", eventCount);//��־����
        context.put("alarmCount", alarmCount);//��Ч�澯��
        context.put("overhaulCount", overhaulEvents.size());//��ҹ������
        context.put("doubtfulCount", doubtfulEventsId.size());//���Ƽ�����
        context.put("doubtfulThreshold", LIMIT_ERROR_NUM);//���Ƽ��޵���ֵ
        context.put("doubtfulTime", LIMIT_ERROR_TIME / 1000 / 60);//���Ƽ��޵�ʱ����ֵ
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
        context.put("stationAlarmList", stationAlarmList);//��վ��澯��
        //��������ͼ����
        List<String> xAxis = new ArrayList<>();//ֱ��ͼX��
        List<String> y1 = new ArrayList<>();//�澯����
        List<String> y2 = new ArrayList<>();//��Ч�澯
        List<String> y3 = new ArrayList<>();//��ҹ����
        List<String> y4 = new ArrayList<>();//���Ƽ���

        lineMap.forEach((x, y) -> {
            xAxis.add(x);
            y1.add(y[0] + "");
            y2.add(y[1] + "");
            y3.add(y[2] + "");
            y4.add(y[3] + "");
        });

        Map<String, List<String>> lineData = new HashMap();//���ڻ�������ͼ
        lineData.put("xData", xAxis);
        lineData.put("all", y1);
        lineData.put("effective", y2);
        lineData.put("overhaul", y3);
        lineData.put("doubtful", y4);
        context.put("lineData", JSON.toJSON(lineData).toString());
        //�����ͼ����
        JSONObject bingData = new JSONObject();
        JSONArray typeList = new JSONArray();

        Map<String, Integer> bing = new HashedMap();
        for (Map.Entry<String, List<EventBean>> entry : deviceErrorNum.entrySet()) {
            String subway = entry.getValue().get(0).getDevice().getSubway();
            if (zhengxian1.contains(subway)) {
                if (bing.containsKey("����һ��Χ")) {
                    bing.put("����һ��Χ", bing.get("����һ��Χ") + entry.getValue().size());
                } else {
                    bing.put("����һ��Χ", entry.getValue().size());
                }
            }
            if (zhengxian2.contains(subway)) {
                if (bing.containsKey("���߶���Χ")) {
                    bing.put("���߶���Χ", bing.get("���߶���Χ") + entry.getValue().size());
                } else {
                    bing.put("���߶���Χ", entry.getValue().size());
                }
            }
            if (zhengxian3.contains(subway)) {
                if (bing.containsKey("������")) {
                    bing.put("������", bing.get("������") + entry.getValue().size());
                } else {
                    bing.put("������", entry.getValue().size());
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

        //�����״�ͼ
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

        //��Σ�豸
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

        //���Ƽ���
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

        //������
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

        //��Σ�豸ͳ��
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


        //�±�
        List<AnalysisBean> fistTable = new ArrayList<>();
        deviceErrorNum.forEach((x, y) -> {
            DeviceBean device = y.get(0).getDevice();
            AnalysisBean analysisBean = new AnalysisBean();
            analysisBean.setSubway(device.getSubway());
            analysisBean.setType(device.getUserDefinedType());
            analysisBean.setDeviceName(device.getName());
            // ��¼�豸 ������ֵ
            if (checkDeviceType(device.getName()) && y.size() >= limitFoucs) {
                Float fenshu = ((float) y.size()) / limitFoucs;
                analysisBean.setAnalysisInfo(String.format("�澯����%d,�����澯��ֵ%d", y.size(), (Math.round(fenshu * 100))) + "%");
                analysisBean.setHandlerInfo("�Ų������������ڵ��Ƿ���ڽ�ͷ�Ӵ�����/�𻵵�����");
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
            // ��Ƶ�����豸
            if (!checkDeviceType(device.getName())) {
                analysisBean.setAnalysisInfo("�豸�澯,����" + y.size());
                analysisBean.setHandlerInfo("��ע�豸�ӵ�/����״̬");
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
        // ������������±�
        doubtfulOverhulEvents.forEach((x) -> {
            List<AnalysisBean> get = getAnalysis(x);
            fistTable.addAll(get);
        });
        context.put("fistTable", fistTable);

        File out = new File(file.getCanonicalPath()+File.separator+fromDate+"���ݱ���.html");
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
                        analysisBean.setType("��Ƶ�����豸");
                        analysisBean.setAnalysisInfo("�豸��" + fusionName.substring(0, fusionName.length() - 1) + timeFormat.format(new Date(eventBean.getTimeStamp())) + "ͬʱ�澯");
                        analysisBean.setHandlerInfo("�����������豸���Ų����ص�Ԫ/�ַ������ӷ������豸�ӵ�/����״̬");
                        analysisBean.setDeviceName(" ");
                        ret.add(analysisBean);


                        ExcelBean excel = new ExcelBean();
                        excel.setSubway(device.getSubway());
                        excel.setDeviceName("��Ƶ�����豸");
                        excel.getAlarmInfo().add("�豸��" + fusionName.toString() + timeFormat.format(new Date(eventBean.getTimeStamp())) + "ͬʱ�澯");
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
                analysisBean.setType("��Ƶ�����豸");
                analysisBean.setAnalysisInfo("�豸��" + fusionName.toString() + timeFormat.format(new Date(list.get(0).getTimeStamp())) + "ͬʱ�澯");
                analysisBean.setHandlerInfo("�����������豸���Ų����ص�Ԫ/�ַ������ӷ������豸�ӵ�/����״̬");
                analysisBean.setDeviceName(" ");
                ret.add(analysisBean);

                ExcelBean excel = new ExcelBean();
                excel.setSubway(device.getSubway());
                excel.setDeviceName("��Ƶ�����豸");
                excel.getAlarmInfo().add("�豸��" + fusionName.toString() + timeFormat.format(new Date(list.get(0).getTimeStamp())) + "ͬʱ�澯");
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
        System.out.println("���ݿ�ʼʱ�䣺" + ExcelUtils.dateTimeFormat.format(new Date(startTime)));
        System.out.println("���ݽ���ʱ�䣺" + ExcelUtils.dateTimeFormat.format(new Date(endTime)));
        System.out.println("���ݿ�ȣ�" + dayCount + "��");
        System.out.println("���Ƽ��޴�����" + doubtfulEventsId.size());
        System.out.println("��Ч������" + alarmCount);

        System.out.println("��ҹ���޸澯��" + overhaulEvents.size());
        System.out.println("---��Ч������---");
        for (Map.Entry<String, List<EventBean>> entry : deviceErrorNum.entrySet()) {
            System.out.println(deviceMapping.get(entry.getKey()).getSubway() + "  " + deviceMapping.get(entry.getKey()).getName() + "  " + entry.getValue().size());
        }
        System.out.println("�����������Ƽ������ݡ�������");
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
        System.out.println("����������ҵ�����澯���ݡ�������");
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
        //������ֹʱ��
        EventBean startEventBean = events.get(0);
        EventBean endEventBean = events.get(events.size() - 1);
        startTime = startEventBean.getTimeStamp();
        endTime = endEventBean.getTimeStamp();
        initLineData(startTime, endTime);
        Long dayStamp = (endTime - startTime) / (1000 * 60 * 60 * 24L);
        eventCount = events.size();
        dayCount = dayStamp.intValue() + 1;//����
        if (dayCount >= 30) {
            limitFoucs = 5;
            System.out.println("��ֵ��" + 5);
        } else {
            limitFoucs = 3;
            System.out.println("��ֵ��" + 3);
        }
        Iterator<EventBean> it = events.iterator();
        //��һ���Ȱ����Ƽ��޺�ȷ�ϵļ����޳������� �豸������Ϣ
        while (it.hasNext()) {
            EventBean event = it.next();
            calendar.setTimeInMillis(event.getTimeStamp());
            String key = getDateKey(calendar);
            //����ͼ �ܸ߸澯���ۼ�1
            lineMap.get(key)[0] = lineMap.get(key)[0] + 1;

            //��֤�����澯�Ƿ��� ��ҹ����
            if (checkDeviceOverhaul(event)) {
                //����ͼ ��ҹ�����ۼ�1
                lineMap.get(key)[2] = lineMap.get(key)[2] + 1;
                it.remove();
                continue;
            }

            checkDoubtfulOverhul(event);
            if (!deviceMapping.containsKey(event.getDevice().getId())) {
                deviceMapping.put(event.getDevice().getId(), event.getDevice());
            }

        }

        //�ٴμ���Ƿ������Ƽ���
        for (Map.Entry<String, List<EventBean>> errorEntry : subwayErrorEvent.entrySet()) {
            if (errorEntry.getValue().size() > LIMIT_ERROR_NUM) {
                for (EventBean bean : errorEntry.getValue()) {
                    doubtfulEventsId.add(bean.getId());
                }
                doubtfulOverhulEvents.add(errorEntry.getValue());
            }
        }

        //�����ȷ�ŵı���
        for (EventBean event : events) {
            calendar.setTimeInMillis(event.getTimeStamp());
            String key = getDateKey(calendar);
            //�豸����
            if (checkDeviceType(event.getDevice().getName())) {
                event.getDevice().setUserDefinedType("��Ƶ��¼�豸");
            } else {
                event.getDevice().setUserDefinedType("��Ƶ�����豸");
            }
            if (!doubtfulEventsId.contains(event.getId())) {//�����Ʊ���
                lineMap.get(key)[1] = lineMap.get(key)[1] + 1;

                //��վ������
                if (stationAlarm.containsKey(event.getDevice().getSubway())) {
                    stationAlarm.put(event.getDevice().getSubway(), stationAlarm.get(event.getDevice().getSubway()) + 1);
                } else {
                    stationAlarm.put(event.getDevice().getSubway(), 1);
                }

                //���豸���Ͷ��ٴ�
                String typeName = event.getDevice().getTypeName();
                if (StringUtils.isEmpty(typeName)) {
                    typeName = "δ֪";
                }
                if (deviceTypeErrorNum.containsKey(typeName)) {
                    deviceTypeErrorNum.put(typeName, deviceTypeErrorNum.get(typeName) + 1);
                } else {
                    deviceTypeErrorNum.put(typeName, 1);
                }

                //�����豸�ж��ٴ�
                if (deviceErrorNum.containsKey(event.getDevice().getId())) {
                    deviceErrorNum.get(event.getDevice().getId()).add(event);
                } else {
                    List<EventBean> alarms = new ArrayList<EventBean>();
                    alarms.add(event);
                    deviceErrorNum.put(event.getDevice().getId(), alarms);
                }
                //�澯���ͷ���
                if (alarm.containsKey(event.getAlarm().getLevel())) {
                    alarm.put(event.getAlarm().getLevel(), alarm.get(event.getAlarm().getLevel()) + 1);
                } else {
                    alarm.put(event.getAlarm().getLevel(), 1);
                }

                //��Ч�澯������
                this.alarmCount++;
            } else {
                //����ͼ ���Ƹ澯+1
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
            if (subwayErrorTime.containsKey(subway)) {//��վ���ֹ�����
                Long oldTime = subwayErrorTime.get(subway);
                if (eventBean.getTimeStamp() - oldTime < LIMIT_ERROR_TIME) {//С��5����
                    subwayErrorEvent.get(subway).add(eventBean);
                    subwayErrorTime.put(subway, eventBean.getTimeStamp());
                } else {//����5���ӣ��ж���һ��
                    if (subwayErrorEvent.get(subway).size() >= LIMIT_ERROR_NUM) {//�����Ƽ���
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
            } else {//��վδ���ֹ�����
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


    //0-4 23�Ժ� �豸���� �ж�Ϊ ��ҹ����
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


