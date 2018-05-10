package club.gaiaProject.sashimi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import club.gaiaProject.sashimi.bean.AlarmBean;
import club.gaiaProject.sashimi.bean.DeviceBean;
import club.gaiaProject.sashimi.bean.EventBean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class ExcelUtils {
    public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final String EXCEL_XLS = "xls";
    private static final String EXCEL_XLSX = "xlsx";

    public static List<EventBean> getDate(File excel, Integer rowNum, Boolean hasHead) throws IOException {
        return getDate(excel, rowNum, 15, hasHead);
    }

    /**
     * 获取excel
     */
    public static List<EventBean> getDate(File excel, Integer rowNum, Integer cellNum, Boolean hasHead) throws IOException {
        Workbook wb = null;
        FileInputStream in = new FileInputStream(excel);
        // 关联execl
        if (excel.getName().endsWith(EXCEL_XLS)) {     //Excel&nbsp;2003
            wb = new HSSFWorkbook(in);
        } else if (excel.getName().endsWith(EXCEL_XLSX)) {    // Excel 2007/2010
            wb = new XSSFWorkbook(in);
        } else {
            System.out.println("文件格式错误！");
            System.exit(-1);
        }
        List<EventBean> ret = new ArrayList<EventBean>();
        //遍历 sheet
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            //取sheet
            Sheet st = wb.getSheetAt(sheetIndex);
            //遍历行
            int rowIndex = 0;
            if (hasHead) rowIndex = 1;
            for (; rowIndex < rowNum; rowIndex++) {

                //取行
                Row row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                EventBean rowInfo = new EventBean();
                DeviceBean device = new DeviceBean();
                AlarmBean alarm = new AlarmBean();
                for (int columnIndex = 0; columnIndex < cellNum; columnIndex++) {
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null) {
                        switch (columnIndex) {
                            case 0:
                                alarm.setLevel((String) getCellValue(cell, false));
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
                rowInfo.setAlarm(alarm);
                rowInfo.setDevice(device);
                rowInfo.setId(rowIndex);
                ret.add(rowInfo);
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
                            System.out.println("日期转换失败");
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
                // 导入时如果为公式生成的数据则无值
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
