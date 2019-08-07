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
        MyLogger.info("׼����ʼ");
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
        // ʹ��InPutStream����ȡproperties�ļ�
        BufferedReader bufferedReader = new BufferedReader(new FileReader(configFilePaht + File.separator + "config.properties"));
        properties.load(bufferedReader);

        if (!properties.containsKey("inputExcel")) {
            MyLogger.error("δ����inputExcel");
            System.exit(-1);
        }
        if (!properties.containsKey("fixEvent")) {
            MyLogger.error("δ����fixEvent");
            System.exit(-1);
        }

        File data = new File(properties.getProperty("inputExcel"));
        File fixData = new File(properties.getProperty("fixEvent"));
        File out = new File(configFilePaht + File.separator + "out");
        if (!data.exists()) {
            MyLogger.error("δ�ҵ��¹ʼ�¼excel");
            System.exit(-1);
        }
        if (!fixData.exists()) {
            MyLogger.error("δ�ҵ����ޱ���excel");
            System.exit(-1);
        }
        if (!out.exists()) {
            out.mkdir();
        }


        Map<String, List<FixEventBean>> fixEvent = ExcelUtils.getFixEvent(fixData);
        MyLogger.info("Ԥ�Ƽ��޼�¼���У�" + fixEvent.size());
        List<EventBean> result = ExcelUtils.getDate(data, true);
        System.out.println("�ܼ�¼��" + result.size());
        DataHandler handler = new DataHandler(result, fixEvent);
        handler.handle();
        String format = handler.createHTML(out);
        File outExcel = new File(out.getCanonicalPath() + File.separator + format + "�������.xls");
        ExcelUtils.writeExcel2(outExcel, handler.getExcelOut());

        MyLogger.info("���������鿴out�ļ���");
    }


}
