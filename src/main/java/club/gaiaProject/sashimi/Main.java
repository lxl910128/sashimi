package club.gaiaProject.sashimi;


import java.io.*;
import java.util.List;

import club.gaiaProject.sashimi.bean.EventBean;
import club.gaiaProject.sashimi.util.ExcelUtils;

/**
 * Created by Administrator on 2018/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\Administrator\\Desktop\\1.xls");
        List<EventBean> result = ExcelUtils.getDate(file, 897, true);
        System.out.println("总记录数"+result.size());
        DataHandler handler = new DataHandler(result);
        handler.handle();
        handler.createHTML();
    }


}
