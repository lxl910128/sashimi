package club.gaiaProject.sashimi;


import java.io.*;
import java.util.List;
import java.util.Map;

import club.gaiaProject.sashimi.bean.EventBean;
import club.gaiaProject.sashimi.bean.FixEventBean;
import club.gaiaProject.sashimi.util.ExcelUtils;
import club.gaiaProject.sashimi.util.MyLogger;

/**
 * Created by Administrator on 2018/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        File data = new File("C:\\Users\\Administrator\\Desktop\\1.xls");
        File fixData = new File("C:\\Users\\Administrator\\Desktop\\2.xls");
        File out = new File("C:\\Users\\Administrator\\Desktop\\out.html");
        File outExcel = new File("C:\\Users\\Administrator\\Desktop\\3.xls");

        Map<String, List<FixEventBean>> fixEvent = ExcelUtils.getFixEvent(fixData);
        MyLogger.info("预计检修记录共有：" + fixEvent.size());
        List<EventBean> result = ExcelUtils.getDate(data, true);
        System.out.println("总记录数" + result.size());
        DataHandler handler = new DataHandler(result, fixEvent);
        handler.handle();
        handler.createHTML(out);
        ExcelUtils.writeExcel(outExcel,handler.getDoubtfulOverhulEvents());
    }


}
