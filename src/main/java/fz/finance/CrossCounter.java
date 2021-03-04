package fz.finance;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class CrossCounter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static int counter;
    private String crossDate = null;
    private SimpleCsvFileWriter simpleCsvFileWriter = new SimpleCsvFileWriter();


    private static final String DEFAULT_PATH = "src\\main\\resources\\stk.txt";


    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH_mm");


    public static String getDefaultPath() {
        return DEFAULT_PATH;
    }

    public void findCrosses(boolean printToFile) {
        findCrosses(DEFAULT_PATH, printToFile);
    }

    public void findCrosses(String filepath, boolean printToFile) {
        findCrosses(makeStkListFromFile(filepath), printToFile);
    }


    public void findCrosses(List<String> symbols, boolean printToFile){

        List<String> golds = new ArrayList<>();
        List<String> deaths = new ArrayList<>();

        List<String> goldCsvList = new ArrayList<>();
        List<String> deathCsvList = new ArrayList<>();

        symbols.parallelStream().forEach( symbol -> {

            counter++;
            int internalCounter = counter;
            System.out.println("STK Symbol: " + symbol + " Counter: " + counter + " has started.");
            int cross = crossChecker(symbol, 15, 50);
            System.out.println("\tSTK Symbol: " + symbol + " Counter: " + internalCounter + " has finished.");


            if (cross == 2){
                goldCsvList.add(crossDate + "," + symbol + "," + String.format("https://finance.yahoo.com/quote/%s?p=%s",symbol, symbol) + "\n");
                golds.add(String.format("%s has made a golden cross.\n", symbol));
                golds.add(String.format("https://finance.yahoo.com/quote/%s?p=%s\n",symbol, symbol));
            } else if (cross == 1) {
                deathCsvList.add(crossDate + "," + symbol + "," + String.format("https://finance.yahoo.com/quote/%s?p=%s",symbol, symbol) + "\n");
                deaths.add(String.format("%s has made a death cross.", symbol));
                deaths.add(String.format("https://finance.yahoo.com/quote/%s?p=%s\n",symbol, symbol));
            }

        });

        System.out.println("Todays golden crosses:\n" + ANSI_YELLOW);

        golds.forEach(System.out::println);

        System.out.println(ANSI_RESET + "Todays death crosses:\n" + ANSI_PURPLE);

        deaths.forEach(System.out::println);

        if (printToFile){
            try {
                writeGoldCSVToFile(goldCsvList);
                writeDeathCSVToFile(deathCsvList);
            } catch (IOException e) {
                System.out.println("Error writing csv file.");
                e.printStackTrace();
            }
        }

        counter = 0;
    }

    private int crossChecker(String stkSymbol, int smolSMADays, int bigSMADays){

        List<HistoricalQuote> histData = getHistoricalData(stkSymbol, bigSMADays);



        double smolSMA = countSMA(histData, smolSMADays, false);
        double smolSMAOffsetDay = countSMA(histData, smolSMADays, true);
        double bigSMA = countSMA(histData, bigSMADays, false);
        double bigSMAOffsetDay = countSMA(histData, bigSMADays, true);

        if (smolSMA == -1 || smolSMAOffsetDay == -1 || bigSMA == -1 || bigSMAOffsetDay == -1){
            return -1;
        }




        if ((smolSMA > bigSMA) && (smolSMAOffsetDay < bigSMAOffsetDay)){
            System.out.format(ANSI_GREEN + "%s has made a golden cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f > %.2f) && (%.2f < %.2f)\n", smolSMA, bigSMA, smolSMAOffsetDay, bigSMAOffsetDay);
            return 2;

        } if ((smolSMA < bigSMA) && (smolSMAOffsetDay > bigSMAOffsetDay)){
            System.out.format(ANSI_PURPLE + "%s has made a death cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f < %.2f) && (%.2f > %.2f)\n", smolSMA, bigSMA, smolSMAOffsetDay, bigSMAOffsetDay);
            return 1;
        }

        return 0;

    }

    public static double countSMA(List<HistoricalQuote> histData, int SMADays, boolean dayOffset){

        if (histData == null){
            return -1D;
        }

        while (histData.get(0).getDate().getTime().before(histData.get(1).getDate().getTime())){
            System.out.println("Reversing historical data list.");
            Collections.reverse(histData);
        }

        double SMACounter = 0;
        int loopCounter = 0;


        try {
            for (int i = (dayOffset ? 1 : 0); i < SMADays + (dayOffset ? 1 : 0); i++) {
                SMACounter = SMACounter + histData.get(i).getClose().doubleValue();
                loopCounter++;
            }
        } catch (NullPointerException e){
            System.out.println("Historical data was not initialized properly. ");
            return -1;
        }

        double SMA = SMACounter / loopCounter;
//        System.out.format("Counted %d entries, SMA is: %.2f$\n", loopCounter, SMA);

        return SMA;

    }

    private List<HistoricalQuote> getHistoricalData(String stkSymbol, int daysPast){


        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        from.add(Calendar.DAY_OF_MONTH, -2 * daysPast);


        Stock stock = null;

        try {
            stock = YahooFinance.get(stkSymbol, from, to, Interval.DAILY);
        } catch (IOException e) {
            System.out.println(ANSI_RED + stkSymbol + " couldn't be initialized." + ANSI_RESET);
            return null;
        }

        List<HistoricalQuote> histData = null;
        try {
            histData = stock.getHistory();
        } catch (IOException e) {
            System.out.println(ANSI_RED +"History for " + stkSymbol + " couldn't be initialized." + ANSI_RESET);
        } catch (NullPointerException e2){
            System.out.println(ANSI_RED +"History for " + stkSymbol + " couldn't be initialized." + ANSI_RESET);
            return null;
        }

        Collections.reverse(histData);

        if (histData.size() < daysPast + 1){
            System.out.println(ANSI_RED + stkSymbol + " is too fresh company to be considered in this timeframe." + ANSI_RESET);
            return null;
        }

        List<HistoricalQuote> readyList = List.copyOf(histData.subList(0, daysPast + 1));

        if (crossDate == null){
            crossDate = DATE_FORMAT.format(readyList.get(0).getDate().getTime());
            System.out.println("crossDate set to " + crossDate);
        }



        return readyList;

    }

    private List<String> makeStkListFromFile(String filePath){

        List<String> stkList = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){

            String stkLine = br.readLine();
            while(stkLine != null){
                stkList.add(stkLine);
                stkLine = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stkList;

    }

    private void writeGoldCSVToFile(List<String> dataList) throws IOException {

        simpleCsvFileWriter.writeCSVToFile(dataList, "symbol,date,link\n", "history\\gold\\", crossDate);
    }

    private void writeDeathCSVToFile(List<String> dataList) throws IOException {

        simpleCsvFileWriter.writeCSVToFile(dataList, "symbol,date,link\n", "history\\death\\", crossDate);
    }



}
