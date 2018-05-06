package club.gaiaProject.sashimi;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import club.gaiaProject.sashimi.bean.EventBean;
import club.gaiaProject.sashimi.util.ExcelUtils;

/**
 * Created by Administrator on 2018/4/17.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\73989\\Desktop\\1.xls");
        List<EventBean> result = ExcelUtils.getDate(file, 897, true);
        System.out.println("总记录数"+result.size());
        DataHandler handler = new DataHandler(result);
        handler.handle();
        handler.print();
    }


}
