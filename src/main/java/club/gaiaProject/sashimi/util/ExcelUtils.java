package club.gaiaProject.sashimi.util;

import club.gaiaProject.sashimi.bean.*;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class ExcelUtils {
    public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final String EXCEL_XLS = "xls";
    private static final String EXCEL_XLSX = "xlsx";

    private static String[] titles = {"�澯�ȼ�", "�澯���", "�豸����", "�豸�ͺ�", "�豸���", "�豸IP", "�豸����", "�澯����", "�豸��ַ", "�澯��Ϣ", "�澯ԭ��", "�澯ʱ��", "�澯ȷ����", "�澯ȷ��˵��", "�澯ȷ��ʱ��"};
    private static String[] titles1 = {"�澯����", "�豸����", "�澯��Ϣ", "�澯����", "ĩ�θ澯ʱ��", "������"};

    public static Workbook getWB(File excel) throws IOException {
        Workbook wb = null;
        FileInputStream in = new FileInputStream(excel);
        // ����execl
        if (excel.getName().endsWith(EXCEL_XLS)) {     //Excel&nbsp;2003
            wb = new HSSFWorkbook(in);
        } else if (excel.getName().endsWith(EXCEL_XLSX)) {    // Excel 2007/2010
            wb = new XSSFWorkbook(in);
        } else {
            System.out.println("�ļ���ʽ����");
            System.exit(-1);
        }
        return wb;
    }

    private static Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap();
        DataFormat dataFormat = wb.createDataFormat();

        // ������ʽ
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setAlignment(CellStyle.ALIGN_CENTER); // ˮƽ����
        titleStyle.setVerticalAlignment(CellStyle.ALIGN_CENTER); // ��ֱ����
        titleStyle.setLocked(true);
        titleStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        titleStyle.setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleFont.setFontName("΢���ź�");
        titleStyle.setFont(titleFont);
        styles.put("title", titleStyle);

        // �ļ�ͷ��ʽ
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setVerticalAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        headerStyle.setWrapText(true);
        Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontName("΢���ź�");
        headerStyle.setFont(headerFont);
        styles.put("header", headerStyle);

        // ������ʽ
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cellStyle.setWrapText(true);
        cellStyle.setBorderRight(CellStyle.BORDER_THIN);
        cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderTop(CellStyle.BORDER_THIN);
        cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        styles.put("cell", cellStyle);

        return styles;
    }

    public static void writeExcel2(File file, List<ExcelBean> list) throws IOException {
        Workbook wb = null;
        if (file.getName().contains(EXCEL_XLS)) {
            wb = new HSSFWorkbook();
        } else {
            wb = new XSSFWorkbook();
        }

        Sheet sheet = wb.createSheet();
        sheet.setDefaultColumnWidth(30);
        Map<String, CellStyle> styles = createStyles(wb);
        /*
         * ����������
         */
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles1.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(styles.get("header"));
            cell.setCellValue(titles1[i]);
        }

        int index = 1;
        for (ExcelBean event : list) {
            Row newRow = sheet.createRow(index);
            for (int i = 0; i < 6; i++) {
                Cell newCell = newRow.createCell(i);
                newCell.setCellStyle(styles.get("cell"));
                switch (i) {
                    case 0:
                        newCell.setCellValue(event.getSubway());
                        break;
                    case 1:
                        newCell.setCellValue(event.getDeviceName());
                        break;
                    case 2:
                        StringBuffer buffer = new StringBuffer();
                        for (String s : event.getAlarmInfo()) {
                            buffer.append(s).append(";");
                        }
                        newCell.setCellValue(buffer.substring(0, buffer.length() - 1));
                        break;
                    case 3:
                        newCell.setCellValue(event.getAlarmCount());
                        break;
                    case 4:
                        newCell.setCellValue(ExcelUtils.dateTimeFormat.format(new Date(event.getLastTime())));
                        break;
                    default:
                        newCell.setCellValue(" ");

                }
            }
            index++;
        }

        // ����ļ�����,��ɾ�����е��ļ�,���´���һ���µ�
        file.createNewFile();
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            wb.write(outputStream);
        } catch (IOException e) {
            MyLogger.error(e.getMessage());
        } finally {
            try {
                if (null != outputStream) {
                    outputStream.close();
                }
            } catch (IOException e) {
                MyLogger.error(e.getMessage());
            }
        }

    }

    public static void writeExcel(File file, List<List<EventBean>> list) throws IOException {
        Workbook wb = null;
        if (file.getName().contains(EXCEL_XLS)) {
            wb = new HSSFWorkbook();
        } else {
            wb = new XSSFWorkbook();
        }


        Sheet sheet = wb.createSheet();
        sheet.setDefaultColumnWidth(15);
        Map<String, CellStyle> styles = createStyles(wb);
        /*
         * ����������
         */
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(styles.get("header"));
            cell.setCellValue(titles[i]);
        }

        int index = 1;
        for (List<EventBean> eventBeans : list) {
            for (EventBean event : eventBeans) {
                Row newRow = sheet.createRow(index);
                Iterator<Cell> cellIterator = event.getRow().cellIterator();
                int cellIndex = 0;
                while (cellIterator.hasNext()) {
                    Cell newCell = newRow.createCell(cellIndex);
                    Cell cell = cellIterator.next();
                    if (cell != null) {
                        newCell.setCellStyle(styles.get("cell"));
                        newCell.setCellValue(cell.getStringCellValue());
                    }

                    cellIndex++;
                }
                index++;
            }
        }
        // ����ļ�����,��ɾ�����е��ļ�,���´���һ���µ�
        file.createNewFile();
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            wb.write(outputStream);
        } catch (IOException e) {
            MyLogger.error(e.getMessage());
        } finally {
            try {
                if (null != outputStream) {
                    outputStream.close();
                }
            } catch (IOException e) {
                MyLogger.error(e.getMessage());
            }
        }

    }

    public static Map<String, List<FixEventBean>> getFixEvent(File excel) throws IOException {
        Workbook wb = getWB(excel);
        Map<String, List<FixEventBean>> ret = new HashMap<>();

        Sheet st = wb.getSheetAt(0);
        Iterator<Row> rowIterator = st.rowIterator();

        int rowIndex = 0;
        while (rowIterator.hasNext()) {
            if (rowIndex == 0) {
                rowIndex++;
                rowIterator.next();
                continue;
            }
            //ȡ��
            Row row = rowIterator.next();
            if (row == null) {
                continue;
            }
            FixEventBean fixEvent = new FixEventBean();
            boolean nullRowFlag = false;
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                Cell cell = row.getCell(columnIndex);

                if (cell != null) {
                    switch (columnIndex) {
                        case 0:
                            Long start = (Long) getCellValue(cell, true);
                            if (start == null) {
                                nullRowFlag = true;
                            }
                            fixEvent.setStartTime(start);
                            break;
                        case 1:
                            Long end = (Long) getCellValue(cell, true);
                            if (end == null) {
                                nullRowFlag = true;
                            }
                            fixEvent.setEndTime(end);
                            break;
                        case 2:
                            String subway = (String) getCellValue(cell, false);
                            if (subway == null) {
                                nullRowFlag = true;
                            }
                            fixEvent.setSubway(subway);
                            break;
                    }
                }
            }
            if (nullRowFlag) {
                continue;
            } else {
                if (fixEvent.getStartTime() <= fixEvent.getEndTime()) {
                    if (ret.containsKey(fixEvent.getSubway())) {
                        ret.get(fixEvent.getSubway()).add(fixEvent);
                    } else {
                        List<FixEventBean> listFix = new ArrayList<>();
                        listFix.add(fixEvent);
                        ret.put(fixEvent.getSubway(), listFix);
                    }
                } else {
                    MyLogger.info(String.format("��%d�����޼�¼�Ŀ�ʼʱ����ڽ���ʱ�䣬����", rowIndex));
                }
            }
            rowIndex++;
        }
        return ret;
    }

    public static List<EventBean> getDate(File excel, Boolean hasHead) throws IOException {
        return getDate(excel, 15, hasHead);
    }

    /**
     * ��ȡexcel
     */
    public static List<EventBean> getDate(File excel, Integer cellNum, Boolean hasHead) throws IOException {
        Workbook wb = getWB(excel);
        List<EventBean> ret = new ArrayList<EventBean>();
        //���� sheet
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            //ȡsheet
            Sheet st = wb.getSheetAt(sheetIndex);
            Iterator<Row> rowIterator = st.rowIterator();
            //������
            int rowIndex = 0;
            while (rowIterator.hasNext()) {
                if (rowIndex == 0 && hasHead) {
                    rowIndex++;
                    rowIterator.next();
                    continue;
                }
                //ȡ��
                Row row = rowIterator.next();
                if (row == null) {
                    continue;
                }

                EventBean rowInfo = new EventBean(row);
                DeviceBean device = new DeviceBean();
                AlarmBean alarm = new AlarmBean();
                boolean nullRowFlag = false;
                for (int columnIndex = 0; columnIndex < cellNum; columnIndex++) {
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null) {
                        switch (columnIndex) {
                            case 0:
                                String level = (String) getCellValue(cell, false);
                                //�����һ��Ϊ������������û����
                                if (level == null) {

                                    nullRowFlag = true;
                                }
                                alarm.setLevel(level);
                                break;
                            case 1:
                                alarm.setType((String) getCellValue(cell, false));
                                break;
                            case 2:
                                device.setName((String) getCellValue(cell, false));
                                break;
                            case 3:
                                device.setTypeID((String) getCellValue(cell, false));
                                break;
                            case 4:
                                device.setId((String) getCellValue(cell, false));
                                break;
                            case 5:
                                device.setIp((String) getCellValue(cell, false));
                                break;
                            case 6:
                                device.setTypeName((String) getCellValue(cell, false));
                                break;
                            case 7:
                                device.setSubway((String) getCellValue(cell, false));
                                break;
                            case 8:
                                device.setAddress((String) getCellValue(cell, false));
                                break;
                            case 9:
                                alarm.setInfo((String) getCellValue(cell, false));
                                break;
                            case 10:
                                alarm.setReason((String) getCellValue(cell, false));
                                break;
                            case 11:
                                rowInfo.setTimeStamp((Long) getCellValue(cell, true));
                                break;
                            case 12:
                                rowInfo.setHandler((String) getCellValue(cell, false));
                                break;
                            case 13:
                                rowInfo.setHandlerInfo((String) getCellValue(cell, false));
                                break;
                            case 14:
                                rowInfo.setHandlerTime((Long) getCellValue(cell, true));
                                break;
                        }
                    }
                }
                if (nullRowFlag) {
                    MyLogger.info(String.format("��%d�����У��ж�Ϊ���в�����", rowIndex + 1));
                } else {
                    rowInfo.setAlarm(alarm);
                    rowInfo.setDevice(device);
                    rowInfo.setId(rowIndex);
                    ret.add(rowInfo);
                }
                rowIndex++;
            }
        }
        ret.sort((x, y) -> {
            return x.getTimeStamp().compareTo(y.getTimeStamp());
        });
        return ret;

    }

    private static Object getCellValue(Cell cell, Boolean isTime) {
        Object value = null;
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                String str = cell.getStringCellValue();
                if (isTime) {
                    if (StringUtils.isNotEmpty(str)) {
                        try {
                            value = dateTimeFormat.parse(str).getTime();
                        } catch (ParseException e) {
                            System.out.println("����ת��ʧ��");
                            value = null;
                        }
                    }
                } else {
                    value = str;
                }
                break;
            case HSSFCell.CELL_TYPE_NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    if (date != null) {
                        value = date.getTime();
                    }
                } else {
                    value = new DecimalFormat("0").format(cell.getNumericCellValue());
                }
                break;
            case HSSFCell.CELL_TYPE_FORMULA:
                // ����ʱ���Ϊ��ʽ���ɵ���������ֵ
                if (!cell.getStringCellValue().equals("")) {
                    value = cell.getStringCellValue();
                } else {
                    value = cell.getNumericCellValue() + "";
                }
                break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                value = (cell.getBooleanCellValue() == true ? "Y" : "N");
                break;
            case HSSFCell.CELL_TYPE_BLANK:
            case HSSFCell.CELL_TYPE_ERROR:
            default:
                value = null;
        }
        return value;
    }

}
