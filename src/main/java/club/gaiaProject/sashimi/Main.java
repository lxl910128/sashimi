package club.gaiaProject.sashimi;


import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import club.gaiaProject.sashimi.bean.EventBean;
import club.gaiaProject.sashimi.bean.FixEventBean;
import club.gaiaProject.sashimi.util.ExcelUtils;
import club.gaiaProject.sashimi.util.MyLogger;

/**
 * Created by Administrator on 2018/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        MyLogger.info("准备开始");
        Thread.sleep(500);
        File file = new File("./");
        String configFilePaht = "";
        //magic bug!
        if (file.getCanonicalPath().contains("bin")) {
            configFilePaht = file.getCanonicalFile().getParentFile().getCanonicalPath();
        } else {
            configFilePaht = file.getCanonicalPath();
        }
        Properties properties = new Properties();
        // 使用InPutStream流读取properties文件
        BufferedReader bufferedReader = new BufferedReader(new FileReader(configFilePaht + File.separator + "config.properties"));
        properties.load(bufferedReader);

        if (!properties.containsKey("inputExcel")) {
            MyLogger.error("未配置inputExcel");
            System.exit(-1);
        }
        if (!properties.containsKey("fixEvent")) {
            MyLogger.error("未配置fixEvent");
            System.exit(-1);
        }

        File data = new File(properties.getProperty("inputExcel"));
        File fixData = new File(properties.getProperty("fixEvent"));
        File out = new File(configFilePaht + File.separator + "out");
        if (!data.exists()) {
            MyLogger.error("未找到事故记录excel");
            System.exit(-1);
        }
        if (!fixData.exists()) {
            MyLogger.error("未找到检修报告excel");
            System.exit(-1);
        }
        if (!out.exists()) {
            out.mkdir();
        }


        Map<String, List<FixEventBean>> fixEvent = ExcelUtils.getFixEvent(fixData);
        MyLogger.info("预计检修记录共有：" + fixEvent.size());
        List<EventBean> result = ExcelUtils.getDate(data, true);
        System.out.println("总记录数" + result.size());
        DataHandler handler = new DataHandler(result, fixEvent);
        handler.handle();
        String format = handler.createHTML(out);
        File outExcel = new File(out.getCanonicalPath() + File.separator + format + "结果分析.xls");
        ExcelUtils.writeExcel2(outExcel, handler.getExcelOut());

        MyLogger.info("分析完成请查看out文件夹");
    }


}
