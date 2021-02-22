package fz.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.BufferedReader;
import java.io.FileReader;
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


    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static int counter = 1;

    public static void main(String[] args) throws IOException {



        Calendar dummyFrom = Calendar.getInstance();
        dummyFrom.add(Calendar.DAY_OF_MONTH, -5);
        Stock stock = YahooFinance.get("TSLA", dummyFrom, Calendar.getInstance(), Interval.DAILY);
        stock.print();
        System.out.println(DATE_FORMAT.format(stock.getHistory().get(stock.getHistory().size() - 1).getDate().getTime()));
        System.out.println(stock.getHistory().get(stock.getHistory().size() - 1).getClose().doubleValue() );


        List<String>stks = makeStkListFromFile(STKS_PATH);

        stks.parallelStream().forEach( stk -> {

            crossCalculator(stk, 15, 50);
            System.out.println("STK Symbol: " + stk + " Counter: " + counter++);

           });

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

//        System.out.println(smolSMA);
//        System.out.println(smolSMAMinusDay);
//        System.out.println(bigSMA);
//        System.out.println(bigSMAMinusDay);

        if ((smolSMA > bigSMA) && (smolSMAMinusDay < bigSMAMinusDay)){
            System.out.format(ANSI_GREEN + "%s has made a golden cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f > %.2f) && (%.2f < %.2f)\n", smolSMA, bigSMA, smolSMAMinusDay, bigSMAMinusDay);
        } if ((smolSMA < bigSMA) && (smolSMAMinusDay > bigSMAMinusDay)){
            System.out.format(ANSI_PURPLE + "%s has made a death cross\n" + ANSI_RESET, stkSymbol);
            System.out.printf("(%.2f < %.2f) && (%.2f > %.2f)\n", smolSMA, bigSMA, smolSMAMinusDay, bigSMAMinusDay);
        }


    }

    public static List<HistoricalQuote> getHistoricalData(String stkSymbol, int daysPast){

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        from.add(Calendar.DAY_OF_MONTH, -2 * daysPast);

//        System.out.println(DATE_FORMAT.format(from.getTime()));
//        System.out.println(DATE_FORMAT.format(to.getTime()));

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
            e.printStackTrace();
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

        return readyList;

    }

}


