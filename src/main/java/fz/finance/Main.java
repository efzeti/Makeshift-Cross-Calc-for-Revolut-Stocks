package fz.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String STKS_PATH = "src\\main\\resources\\stk.txt";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static String golds = ANSI_RESET + ANSI_YELLOW;
    public static String deaths = ANSI_RESET + ANSI_PURPLE;
    public static String crossDate = null;

    public static List<String> goldCsvList = new ArrayList<>();
    public static List<String> deathCsvList = new ArrayList<>();


    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static int counter = 0;

    public static void main(String[] args) throws IOException {


        goldCsvList.add("symbol,date,link\n");
        deathCsvList.add("symbol,date,link\n");


        Calendar dummyFrom = Calendar.getInstance();
        dummyFrom.add(Calendar.DAY_OF_MONTH, -5);
        Stock stock = YahooFinance.get("TSLA", dummyFrom, Calendar.getInstance(), Interval.DAILY);
        stock.print();
        System.out.println(DATE_FORMAT.format(stock.getHistory().get(stock.getHistory().size() - 1).getDate().getTime()));
        System.out.println(stock.getHistory().get(stock.getHistory().size() - 1).getClose().doubleValue());


        List<String>stks = makeStkListFromFile(STKS_PATH);

        Calendar now = Calendar.getInstance();



        stks.parallelStream().forEach( stk -> {

            counter++;
            int internalCounter = counter;
            System.out.println("STK Symbol: " + stk + " Counter: " + counter + " has started.");
            crossCalculator(stk, 15, 50);
            System.out.println("STK Symbol: " + stk + " Counter: " + internalCounter + " has finished.");

           });

        System.out.format("\n" + ANSI_CYAN + "Todays results (%s):\n",crossDate);
        System.out.println(ANSI_YELLOW + golds + ANSI_RESET);
        System.out.println(ANSI_PURPLE + deaths + ANSI_RESET);


        System.out.println(TIME_FORMAT.format(now.getTime()));
        System.out.println(TIME_FORMAT.format(Calendar.getInstance().getTime()));

        FileWriter goldCsvWriter = new FileWriter("history\\gold\\" + crossDate + ".csv");
        FileWriter deathCsvWriter = new FileWriter("history\\death\\" + crossDate + ".csv");


        for (String row : goldCsvList){
            goldCsvWriter.append(row);
        }

        for (String row : deathCsvList){
            deathCsvWriter.append(row);
        }

        goldCsvWriter.flush();
        goldCsvWriter.close();
        deathCsvWriter.flush();
        deathCsvWriter.close();
    }

    public static List<String> makeStkListFromFile(String filePath){

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

    public static void printStks(List<String> stks){
        List<String> wrongStks = new ArrayList<>();

        stks.parallelStream().forEach( stk -> {

            Stock streamStock = null;

            try {
                streamStock = YahooFinance.get(stk);
            } catch (IOException ex) {
                System.out.println(ANSI_RED + "Cannot initialize: " + stk + ANSI_RESET);
            }

            if (streamStock != null){
                if (streamStock.getQuote().getPrice() != null){
                    BigDecimal streamPrice = streamStock.getQuote().getPrice();

                    System.out.format("%-5s is valued at " + streamPrice + "$\n", stk);
                } else {
                    System.out.println(ANSI_RED + stk + " initialized properly but the price is null." + ANSI_RESET);
                    wrongStks.add(stk);
                }

            } else {
                wrongStks.add(stk);
            }});

        System.out.println(ANSI_RED + "Wrong stocks: " + wrongStks + ANSI_RESET);
    }

    public static double countSMA(String stkSymbol, int SMADays, boolean dayMinus){
        List<HistoricalQuote> histData = getHistoricalData(stkSymbol, SMADays);
        return countSMA(histData, SMADays, dayMinus);
    }

    public static double countSMA(List<HistoricalQuote> histData, int SMADays, boolean dayMinus){

        if (histData == null){
            return -1D;
        }

        while (histData.get(0).getDate().getTime().before(histData.get(1).getDate().getTime())){
            System.out.println("Reversing historical data list.");
            Collections.reverse(histData);
        }

        double SMACounter = 0;
        int loopCounter = 0;


        for (int i = (dayMinus ? 1 : 0); i < SMADays + (dayMinus ? 1 : 0); i++){
            SMACounter = SMACounter + histData.get(i).getClose().doubleValue();
            loopCounter++;
        }

        double SMA = SMACounter / loopCounter;
//        System.out.format("Counted %d entries, SMA is: %.2f$\n", loopCounter, SMA);

        return SMA;

    }

    public static void crossCalculator(String stkSymbol, int smolSMADays, int bigSMADays){

        List<HistoricalQuote> histData = getHistoricalData(stkSymbol, bigSMADays);



        double smolSMA = countSMA(histData, smolSMADays, false);
        double smolSMAMinusDay = countSMA(histData, smolSMADays, true);
        double bigSMA = countSMA(histData, bigSMADays, false);
        double bigSMAMinusDay = countSMA(histData, bigSMADays, true);




        if ((smolSMA > bigSMA) && (smolSMAMinusDay < bigSMAMinusDay)){
            System.out.format(ANSI_GREEN + "%s has made a golden cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f > %.2f) && (%.2f < %.2f)\n", smolSMA, bigSMA, smolSMAMinusDay, bigSMAMinusDay);

            golds = golds + String.format("%s has made a golden cross\n", stkSymbol);
            golds = golds + String.format("https://finance.yahoo.com/quote/%s?p=%s\n",stkSymbol, stkSymbol);

            goldCsvList.add(crossDate + "," + stkSymbol + "," + String.format("https://finance.yahoo.com/quote/%s?p=%s",stkSymbol, stkSymbol) + "\n");


        } if ((smolSMA < bigSMA) && (smolSMAMinusDay > bigSMAMinusDay)){
            System.out.format(ANSI_PURPLE + "%s has made a death cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f < %.2f) && (%.2f > %.2f)\n", smolSMA, bigSMA, smolSMAMinusDay, bigSMAMinusDay);

            deaths = deaths + String.format("%s has made a death cross\n", stkSymbol);
            deaths = deaths + String.format("https://finance.yahoo.com/quote/%s?p=%s\n",stkSymbol, stkSymbol);

            deathCsvList.add(crossDate + "," + stkSymbol + "," + String.format("https://finance.yahoo.com/quote/%s?p=%s",stkSymbol, stkSymbol) + "\n");
        }


    }

    public static List<HistoricalQuote> getHistoricalData(String stkSymbol, int daysPast){


        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        from.add(Calendar.DAY_OF_MONTH, -2 * daysPast);


        Stock stock = null;
        try {
            stock = YahooFinance.get(stkSymbol, from, to, Interval.DAILY);
        } catch (IOException e) {
            System.out.println(ANSI_RED + stkSymbol + " couldn't be initialized." + ANSI_RESET);
//            e.printStackTrace();
//            System.out.println(e.getMessage());
            return null;
        }

        List<HistoricalQuote> histData = null;
        try {
            histData = stock.getHistory();
        } catch (IOException e) {
//            e.printStackTrace();
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

}


