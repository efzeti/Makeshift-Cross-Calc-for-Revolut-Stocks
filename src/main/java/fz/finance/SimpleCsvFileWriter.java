package fz.finance;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class SimpleCsvFileWriter {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH_mm");



    public void writeCSVToFile(List<String> dataList, String head, String filePath, String fileName) throws IOException {

        dataList.add(0, head);

        FileWriter CsvWriter = new FileWriter(filePath + fileName + ".csv");

        for (String row : dataList){
            CsvWriter.append(row);
        }

        CsvWriter.flush();
        CsvWriter.close();


    }


}
